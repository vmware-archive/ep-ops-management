/*
 * Copyright (c) 2015 VMware, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.hyperic.hq.bizapp.client;

import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.AskQuestionsUtil;
import org.hyperic.hq.bizapp.agent.client.AskQuestionsUtil.AutoQuestionException;
import org.hyperic.hq.bizapp.agent.client.AskQuestionsUtil.UserQuestionException;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_result;
import org.hyperic.hq.bizapp.shared.lather.ServerInfo_result;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.hyperic.lather.client.LatherHTTPClient;
import org.hyperic.util.http.AgentRequest;
import org.hyperic.util.http.AgentRequest.AgentHttpMethod;
import org.hyperic.util.http.ServerHttpClient;
import org.hyperic.util.security.CertificateService;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.KeystoreManager;

public class BizappCallbackClient extends AgentCallbackClient {
    private static final Log log = LogFactory.getLog(BizappCallbackClient.class);
    private static final String INVALID_CERT_IN_CHAIN_MSG =
                "Unable to complete agent setup. An unverified certificate without a valid chain was presented.";
    private static final String EXPIRED_CERT_IN_CHAIN_MSG =
                "Unable to complete agent setup. The certificate presented by the server has expired.";
    private static final String NOT_YET_VALID_CERT_IN_CHAIN_MSG =
                "Unable to complete agent setup. The certificate presented by the server is not valid.";
    private final KeystoreConfig keystoreConfig;

    public BizappCallbackClient(ProviderFetcher fetcher,
                                AgentConfig bootConfig) {
        super(fetcher, bootConfig);

        // configure lather proxy settings
        if (bootConfig.isProxyServerSet()) {
            log.info("Setting proxy server: host=" + bootConfig.getProxyIp() +
                        "; port=" + bootConfig.getProxyPort());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYHOST,
                        bootConfig.getProxyIp());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYPORT,
                        String.valueOf(bootConfig.getProxyPort()));
        }

        String keyStorePropName = AgentConfig.PROP_KEYSTORE_PATH[0];

        System.setProperty(keyStorePropName, bootConfig.getBootProperties().getProperty(keyStorePropName));
        this.keystoreConfig = new AgentKeystoreConfig(bootConfig);
    }

    /**
     * @param cachedCertificatesChain - a cached approved unverified certificate. or null if there isn't. caching help
     *            us avoid asking the user to approve again and again, in cases of server not being available.
     */
    public void bizappServerInfo(X509Certificate[] cachedCertificatesChain)
        throws AgentCallbackClientException, IOException, AutoQuestionException, UserQuestionException {
        ProviderInfo provider = this.getProvider();
        String address = provider.getProviderAddress();
        if (address == null) {
            throw new IllegalStateException("Remote address is empty.");
        }

        BizappCallbackHTTPClient httpClient;
        try {
            httpClient = createBizappHttpClient(false);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        }
        LatherHTTPClient latherClient =
                    new LatherHTTPClient(address, httpClient);
        HttpResponse serverInfoHttpResponse =
                    this.invokeUnparsedLatherCall(provider, CommandInfo.CMD_SERVER_INFO, NullLatherValue.INSTANCE,
                                latherClient, true);
        if (serverInfoHttpResponse == null) {
            // perform retries
            throw new AgentCallbackClientException("Got null server info response");
        }

        X509Certificate[] chain = httpClient.getAndResetUnverifiedCertificatesChain();
        if (chain == null) { // The certificate was verified so we can continue
            // Parse the httpResponse ignoring the return value,
            // this is done in order to make sure the error header is empty.
            // Otherwise, the failure would occur only later during agent setup.
            // In case of an error an AgentCallbackClientException is thrown.
            getServerInfoResult(latherClient, serverInfoHttpResponse, chain);
            return;
        }

        if (!Arrays.equals(chain, cachedCertificatesChain)) {
            approveCertificate(chain);
        }

        ServerInfo_result serverInfoResult = getServerInfoResult(latherClient, serverInfoHttpResponse, chain);
        boolean isCustomCertificate = serverInfoResult.getIsCustomCertificate();
        log.info(String.format("Is custom certificate: %s", isCustomCertificate));

        int certificateChainIndex = getCertificateChainIndex(isCustomCertificate);

        if (!isCustomCertificate && chain.length < 2) {
            System.out.println(INVALID_CERT_IN_CHAIN_MSG);
            throw new AutoQuestionException(INVALID_CERT_IN_CHAIN_MSG);
        }

        importServerCertificate(chain[certificateChainIndex], getAgentConfig());
    }

    private BizappCallbackHTTPClient createBizappHttpClient(boolean isSecure)
        throws KeyStoreException, IOException {
        X509TrustManager customTrustManager = KeystoreManager.getCustomTrustManager(keystoreConfig);
        BizappCallbackHTTPClient httpClient =
                    new BizappCallbackHTTPClient(keystoreConfig, customTrustManager, isSecure);
        return httpClient;
    }

    private ServerInfo_result getServerInfoResult(LatherHTTPClient latherClient,
                                                  HttpResponse serverInfoHttpResponse,
                                                  X509Certificate[] chain)
        throws AgentCallbackClientException {
        ServerInfo_result serverInfoResult;
        try {
            // We parse this data only after the user has accepted the certificate
            serverInfoResult = (ServerInfo_result) latherClient.parseLatherHttpResponse(serverInfoHttpResponse);
        } catch (LatherRemoteException e) {
            throw new AgentCallbackClientException(new CachedCertificateException(chain, e));
        } catch (IOException e) {
            throw new AgentCallbackClientException(e);
        }
        return serverInfoResult;
    }

    private void approveCertificate(X509Certificate[] chain)
        throws AutoQuestionException, AgentCallbackClientException, UserQuestionException {
        X509Certificate unverifiedCertificate = chain[0];
        String untrustedCertificateMsg =
                    "\nThe server has presented an untrusted certificate. "
                                + "\nVerify that the certificate below matches that of the Certificate Authority (CA) for your "
                                + HQConstants.PRODUCT
                                + " Manager's cluster."
                                + "\nThe CA is the direct signer of the certificates for the "
                                + HQConstants.PRODUCT
                                + " nodes.\nThe agent must trust the CA certificate to connect to any node of the cluster.\n"
                                + CertificateService.printCert(unverifiedCertificate)
                                + "\n\nFor more information on the certificate chain, choose the \"more\" option below.";
        System.out.println(untrustedCertificateMsg);
        log.info(untrustedCertificateMsg);

        AskQuestionsUtil askQuestionsUtil = new AskQuestionsUtil(getAgentConfig());
        String certificateQuestion = "Do you trust this certificate";
        Boolean isTrustCertificateAnswer = false;

        try {
            isTrustCertificateAnswer =
                        askQuestionsUtil.askYesNoMoreQuestion(certificateQuestion, false, null);
        } catch (IOException ex) {
            throw new AgentCallbackClientException(ex);
        }

        if (isTrustCertificateAnswer == null) { // user answered 'more'
            for (int i = 0; i < chain.length; i++) {
                untrustedCertificateMsg = CertificateService.printCert(chain[i]);
                System.out.println(untrustedCertificateMsg);
                log.info(untrustedCertificateMsg);
            }
            System.out.println();
            try {
                isTrustCertificateAnswer = askQuestionsUtil.askYesNoQuestion(certificateQuestion, false, null);
            } catch (IOException ex) {
                throw new AgentCallbackClientException(ex);
            }
        }

        if (!isTrustCertificateAnswer) { // if answer is no
            throw new UserQuestionException("The certificate was rejected by the user");
        }

        // The certificate wasn't verified but the user chose to trust it (answered yes).
    }

    private int getCertificateChainIndex(boolean isCustomCertificate) {
        return isCustomCertificate ? 0 : 1;
    }

    /*
     * Return a certificate from the given chain, matching the given thumbprint
     * while ignoring case, colon/space delimiters and supporting SHA1/SHA256/MD5 algorithms
     * Return null if non exist.
     */
    private static X509Certificate getCertificateByThumbprint(X509Certificate[] unverifiedCertificatesChain,
                                                              String certificateThumbprint) {
        log.info("The following server certificate thumbprint will be trusted during registration: "
                    + certificateThumbprint);
        for (X509Certificate certificate : unverifiedCertificatesChain) {
            if (CertificateService.compareCertificateToThumbprint(certificate, certificateThumbprint)) {
                String trustServerCertificateThumbprintMsg =
                            "Found a certificate thumbprint that matches the one provided in the properties file.";
                System.out.println(trustServerCertificateThumbprintMsg);
                log.info(trustServerCertificateThumbprintMsg);
                return certificate;
            }
        }
        return null;
    }

    /**
     * Register an agent with the server.
     * 
     * @param user The user name for connecting the agent to the server.
     * @param pword The password for connecting the agent to the server.
     * @param agentIP The agent IP address.
     * @param version The version.
     * @param cpuCount The host platform cpu count.
     * @param acceptCertificates <code>true</code> if the server should accept agent SSL certificates
     * @param csr Certificate Signing Request containing the agent token as the CN
     * @return The result containing the new agent token.
     */
    public RegisterAgentResult registerAgent(String user,
                                             String pword,
                                             String agentIP,
                                             String version,
                                             int cpuCount,
                                             byte[] csr)
        throws AgentCallbackClientException {
        if (ArrayUtils.isEmpty(csr)) {
            logAndThrowRegisterAgentNullArgumentError("certificate signing request", "csr");
        }

        RegisterAgent_result res;
        RegisterAgent_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new RegisterAgent_args();
        args.setUser(user);
        args.setPword(pword);
        args.setAgentIP(agentIP);
        args.setVersion(version);
        args.setCpuCount(cpuCount);
        args.setCertificateRequest(csr);

        BizappCallbackHTTPClient httpClient;
        try {
            httpClient = createBizappHttpClient(true);
        } catch (KeyStoreException e) {
            throw new AgentCallbackClientException(e);
        } catch (IOException e) {
            throw new AgentCallbackClientException(e);
        }
        LatherHTTPClient latherClient =
                    new LatherHTTPClient(provider.getProviderAddress(), httpClient);

        res = (RegisterAgent_result) this.invokeLatherCall(provider,
                    CommandInfo.CMD_REGISTER_AGENT,
                    args, latherClient, true);
        return new RegisterAgentResult(res.getCertificate(), res.getErrorMessage());
    }

    private void logAndThrowRegisterAgentNullArgumentError(String argumentDescription,
                                                           String argumentName) {
        log.error("Register agent called with no " + argumentDescription);
        throw new NullArgumentException(argumentName);
    }

    public static void importServerCertificate(X509Certificate certificate,
                                               AgentConfig agentConfig) {
        KeystoreManager keystoreManager = KeystoreManager.getKeystoreManager();
        AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig(agentConfig.getBootProperties());
        keystoreManager.importServerCert(keystoreConfig, certificate);
    }

    protected HttpResponse invokeUnparsedLatherCall(ProviderInfo provider,
                                                    String methodName,
                                                    LatherValue args,
                                                    LatherHTTPClient latherClient,
                                                    boolean closeConn)
        throws AgentCallbackClientException {
        String addr = provider.getProviderAddress();

        try {
            HttpResponse rtn = latherClient.invokeUnparsed(methodName, args, closeConn);
            return rtn;
        } catch (ConnectException exc) {
            final String eMsg = "Unable to contact server @ " + addr + ": " + exc;
            log.debug(eMsg);
            throw new AgentCallbackClientException(eMsg);
        } catch (LatherRemoteException exc) {
            final String eMsg = "Remote error while invoking '" + methodName + ": " + exc;
            log.debug(eMsg);
            throw new AgentCallbackClientException(eMsg, exc);
        } catch (Exception exc) {
            log.debug(exc);
            throw new AgentCallbackClientException(exc);
        }
    }

    private static class BizappCallbackHTTPClient extends ServerHttpClient {
        private final Log log = LogFactory.getLog(BizappCallbackHTTPClient.class);
        private final ServerInfoCommandTrustStrategy insecureTrustStrategy;

        public BizappCallbackHTTPClient(KeystoreConfig keystoreConfig,
                                        X509TrustManager defaultTrustManager,
                                        boolean isSecure) {
            super(keystoreConfig, LatherHTTPClient.getHttpConfig(
                        LatherHTTPClient.TIMEOUT_CONN, LatherHTTPClient.TIMEOUT_DATA),
                        false, LatherHTTPClient.getMaxRequestsPerConnection(),
                        LatherHTTPClient.isSupportRRDNS(),
                        LatherHTTPClient.getFailPeriodInMin(),
                        LatherHTTPClient.getDownPeriodInMin());
            if (!isSecure) {
                insecureTrustStrategy = new ServerInfoCommandTrustStrategy(defaultTrustManager);
                log.debug("Create BizappCallbackHTTPClient - not secure mode");
                SSLSocketFactory sslSocketFactory = getServerInfoCommandSSLSocketFactory(defaultTrustManager);
                getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, sslSocketFactory));
            } else {
                insecureTrustStrategy = null;
                log.debug("Create BizappCallbackHTTPClient - secure mode");

            }

        }

        @Override
        public HttpResponse post(String url,
                                 Map<String, String> params,
                                 boolean closeConn)
            throws ClientProtocolException, IOException {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Method", params.get("method"));
            AgentRequest request = new AgentRequest(url, AgentHttpMethod.POST);
            request.setHeaders(headers);
            request.setParams(params);
            return this.serversManager.sendTryAll(request);
        }

        public X509Certificate[] getAndResetUnverifiedCertificatesChain() {
            return insecureTrustStrategy.getAndResetUnverifiedCertificatesChain();
        }

        private SSLSocketFactory getServerInfoCommandSSLSocketFactory(X509TrustManager defaultTrustManager) {
            SSLSocketFactory sslSocketFactory;
            try {
                sslSocketFactory = new SSLSocketFactory(insecureTrustStrategy, new AllowAllHostnameVerifier());
            } catch (Exception e) {
                log.error(e);
                throw new RuntimeException(e);
            }
            return sslSocketFactory;
        }
    }

    private static class ServerInfoCommandTrustStrategy implements TrustStrategy {

        private X509Certificate[] unverifiedCertificatesChain = null;
        private final X509TrustManager trustManager;

        public ServerInfoCommandTrustStrategy(X509TrustManager trustManager) {
            this.trustManager = trustManager;
        }

        public boolean isTrusted(
                                 final X509Certificate[] chain,
                                 String authType)
            throws CertificateException {
            setUnverifiedCertificatesChain(null);
            if (chain == null || chain.length < 1) {
                System.out.println(INVALID_CERT_IN_CHAIN_MSG);
                throw new CertificateException(INVALID_CERT_IN_CHAIN_MSG);
            }

            try {
                trustManager.checkServerTrusted(chain, authType);
                return true;
            } catch (CertificateExpiredException e) {
                System.out.println(EXPIRED_CERT_IN_CHAIN_MSG);
                log.error(EXPIRED_CERT_IN_CHAIN_MSG, e);
                throw new CertificateException(EXPIRED_CERT_IN_CHAIN_MSG, e);
            } catch (CertificateNotYetValidException e) {
                System.out.println(NOT_YET_VALID_CERT_IN_CHAIN_MSG);
                log.error(NOT_YET_VALID_CERT_IN_CHAIN_MSG, e);
                throw new CertificateException(NOT_YET_VALID_CERT_IN_CHAIN_MSG, e);
            } catch (CertificateException certException) {
                String serverCertificateThumbprintPropValue =
                            getAgentConfig().getBootProperty("agent.setup.serverCertificateThumbprint");
                X509Certificate autoVerifiedCertificate = null;
                if (StringUtils.isNotEmpty(serverCertificateThumbprintPropValue)) {
                    serverCertificateThumbprintPropValue =
                                removeNonValidCharsFromThumb(serverCertificateThumbprintPropValue);
                    autoVerifiedCertificate = getCertificateByThumbprint(chain, serverCertificateThumbprintPropValue);
                    if (autoVerifiedCertificate != null) {
                        importServerCertificate(autoVerifiedCertificate, getAgentConfig());
                        return true; // The certificate of the thumbprint was imported.
                    }
                    throw certException;
                }

                // We will ask the user so we have to store the chain
                setUnverifiedCertificatesChain(chain);
                return true;
            }
        }

        /**
         * When a thumbprint is copied from Chrome or MS Explorer into agent.properties, sometimes some invisible
         * character are copies as well. The user is not always aware of it. In order to prevent the user the need to
         * troubleshoot the above, any character that is not valid for the certificate is removed
         */
        private String removeNonValidCharsFromThumb(String serverCertificateThumbprint) {
            return serverCertificateThumbprint.replaceAll("[^\\w\\s]", "");
        }

        private synchronized void setUnverifiedCertificatesChain(X509Certificate[] unverifiedCertificatesChain) {
            this.unverifiedCertificatesChain = unverifiedCertificatesChain;
        }

        public synchronized X509Certificate[] getAndResetUnverifiedCertificatesChain() {
            X509Certificate[] chain = unverifiedCertificatesChain;
            unverifiedCertificatesChain = null;
            return chain;
        }
    }

}
