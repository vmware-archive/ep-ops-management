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

package org.hyperic.hq.bizapp.agent.client;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.bouncycastle.operator.OperatorCreationException;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConfigException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.LegacyAgentCommandsClientImpl;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentDaemon.RunnableAgent;
import org.hyperic.hq.agent.server.LoggingOutputStream;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.bizapp.agent.PlatformToken;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.TokenNotFoundException;
import org.hyperic.hq.bizapp.agent.client.AskQuestionsUtil.AutoQuestionException;
import org.hyperic.hq.bizapp.agent.client.AskQuestionsUtil.UserQuestionException;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.BizappCallbackClient;
import org.hyperic.hq.bizapp.client.CachedCertificateException;
import org.hyperic.hq.bizapp.client.RegisterAgentResult;
import org.hyperic.hq.bizapp.client.StaticProviderFetcher;
import org.hyperic.hq.bizapp.client.common.CommonServerInteractor;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.util.properties.PropertiesUtil;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileWatcher;
import org.hyperic.sigar.FileWatcherThread;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.PropertyEncryptionUtil;
import org.hyperic.util.PropertyUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.exec.Os;
import org.hyperic.util.security.CertificateService;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.KeystoreManager;
import org.hyperic.util.security.SecurityUtil;
import org.hyperic.util.vmware.VMwareGuestInfo;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * This class provides the command line entry point into dealing with the agent.
 */
public class AgentClient {
    private static final PrintStream SYSTEM_ERR = System.err;
    private static final PrintStream SYSTEM_OUT = System.out;

    private static final String REGISTRATION_URL_SUFFIX = "-registration";

    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final String LOG_PATTERN_LAYOUT = "%d %-5p [%t] [%c{1}] %m%n";
    private static final int BUFFER_SIZE = 1024;
    private static final String MAX_FILE_SIZE = "5000KB";
    private static final int MAX_FILES = 1;

    private static final String PROP_LOGFILE = "agent.logFile";
    private static final String PROP_STARTUP_TIMEOUT = "agent.startupTimeOut";
    private static final int AGENT_STARTUP_TIMEOUT = (60 * 5); // 5 min
    private static final String PROP_SERVER_CONNECTION_TIMEOUT = "agent.setup.serverConnectionTimeout";
    private static final int SERVER_CONNECTION_TIMEOUT_DEFAULT = (60 * 5); // 5 min

    private static final int FORCE_SETUP = -42;
    private static final int LOGIN_ATTEMPTS_LIMIT = 5;
    private static final int TOKEN_LOAD_ATTEMPTS_LIMIT = 3;

    private final AgentCommandsClient agtCommands;
    private final CommandsClient camCommands;
    private final AgentConfig config;
    private final Log log;
    private boolean redirectedOutputs = false;
    private final AskQuestionsUtil askQuestionUtil;
    private static Thread agentDaemonThread;

    private final static String PING = "ping";
    private final static String DIE = "die";
    private final static String START = "start";
    private final static String STATUS = "status";
    private final static String RESTART = "restart";
    private final static String SETUP = "setup";
    private final static String SETUP_IF_NO_PROVIDER = "setup-if-no-provider";
    private final static String SET_PROPERTY = "set-property";
    private final static String UNTRUSTED_CERTIFICATE_MSG =
                "The authenticity of the host cannot be established for one of the following reasons:" +
                            "\n1. An untrusted certificate was presented." +
                            "\n2. The host is unreachable." +
                            "\n3. The agent machine or the host are overloaded." +
                            "\nVerify the serverIP and serverCertificateThumbprint agent properties and retry.";

    private final static String INVALID_ENTRY_AUTO_MESSAGE =
                "The agent could not connect to the specified hostname/IP address and port. The current provider host/port is invalid.";

    private static final String VM_UNIQUE_IDENTIFIER_ENV_KEY_NAME = "vrealize_operations_agent_id";

    private AgentClient(AgentConfig config,
                        SecureAgentConnection conn) {
        this.agtCommands = new LegacyAgentCommandsClientImpl(conn);
        this.camCommands = new CommandsClient(conn);
        this.config = config;
        this.log = LogFactory.getLog(AgentClient.class);
        this.askQuestionUtil = new AskQuestionsUtil(config);
    }

    private long cmdPing(int numAttempts)
        throws AgentConnectionException, AgentRemoteException {
        AgentConnectionException lastExc;

        lastExc = new AgentConnectionException("Failed to connect to agent");
        while (numAttempts-- != 0) {
            try {
                return this.agtCommands.ping();
            } catch (AgentConnectionException exc) {
                // Loop around to the next attempt
                lastExc = exc;
            }
            try {
                if (numAttempts > 0) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException exc) {
                throw new AgentConnectionException("Connection interrupted");
            }
        }
        throw lastExc;
    }// cmdPing

    private void cmdStatus()
        throws AgentConnectionException, AgentRemoteException
    {
        ProviderInfo pInfo;
        String address;
        String currentAgentBundle;

        try {
            currentAgentBundle = this.agtCommands.getCurrentAgentBundle();

            pInfo = this.camCommands.getProviderInfo();
        } catch (AgentConnectionException exc) {
            SYSTEM_ERR.println("Unable to contact agent: " + exc.getMessage());
            return;
        } catch (AgentRemoteException exc) {
            SYSTEM_ERR.println("Error executing the remote method: " +
                        exc.getMessage());
            return;
        }
        SYSTEM_OUT.println("Current agent bundle: " + currentAgentBundle);

        if (pInfo == null || (address = pInfo.getProviderAddress()) == null) {
            SYSTEM_OUT.println("Agent not yet setup");
            return;
        }
        SYSTEM_OUT.println("Agent token: " + pInfo.getAgentToken());

        try {
            String proto;

            URL url = new URL(address);

            SYSTEM_OUT.println("Server IP address: " + url.getHost());
            proto = url.getProtocol();
            if (proto.equalsIgnoreCase("https")) {
                SYSTEM_OUT.print("Server (SSL) port: ");
            } else {
                SYSTEM_OUT.print("Server port:       ");
            }
            SYSTEM_OUT.println(url.getPort());
            SYSTEM_OUT.println("Using new transport; unidirectional=true");
        } catch (Exception exc) {
            SYSTEM_OUT.println("Unable to parse provider info (" +
                        address + "): " + exc.getMessage());
        }

        SYSTEM_OUT.println("Agent listen port: " +
                    this.config.getListenPort());

        if (this.config.isProxyServerSet()) {
            SYSTEM_OUT.println("Proxy server IP address: " + this.config.getProxyIp());
            SYSTEM_OUT.println("Proxy server port: " + this.config.getProxyPort());
        }
    }

    private void cmdDie(int waitTime)
        throws AgentConnectionException, AgentRemoteException
    {
        try {
            this.agtCommands.die();
        } catch (AgentConnectionException exc) {
            return; // If we can't connect then we know the agent is dead
        } catch (AgentRemoteException exc) {
            throw new AgentRemoteException("Error making the remote agent call: " +
                        exc.getMessage());
        }

        // Loop waiting to see if it died before returning
        while (waitTime-- != 0) {
            try {
                this.agtCommands.ping();
            } catch (AgentConnectionException exc) {
                return; // Success!
            } catch (AgentRemoteException exc) {
                exc.printStackTrace(SYSTEM_ERR);
                throw exc; // Something bizarre occurred
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException exc) {
                throw new AgentConnectionException("Connection interrupted");
            }
        }

        throw new AgentRemoteException("Unable to kill agent within timeout");
    }

    private void cmdRestart()
        throws AgentConnectionException, AgentRemoteException {
        try {
            this.agtCommands.restart();
        } catch (AgentConnectionException exc) {
            throw new AgentConnectionException("Unable to connect to agent: " +
                        "already dead?");
        } catch (AgentRemoteException exc) {
            throw new AgentRemoteException("Error making the remote agent call: " +
                        exc.getMessage());
        }
    }

    private BizappCallbackClient testProvider(String provider,
                                              X509Certificate[] cachedCertificatesChain)
        throws AgentCallbackClientException, AutoQuestionException, IOException, UserQuestionException {
        StaticProviderFetcher fetcher;
        BizappCallbackClient res;

        fetcher = new StaticProviderFetcher(new ProviderInfo(provider,
                    "no-auth"));
        res = new BizappCallbackClient(fetcher, config);
        res.bizappServerInfo(cachedCertificatesChain);
        return res;
    }

    /**
     * Test the connection information.
     * 
     * @param provider
     * @return BizappCallbackClient
     * @throws AutoQuestionException
     * @throws AgentCallbackClientException
     * @throws IOException
     * @throws UserQuestionException
     */
    private BizappCallbackClient testConnectionToProvider(String provider)
        throws AutoQuestionException, IOException,
        UserQuestionException {
        BizappCallbackClient bizapp;
        Properties bootP = this.config.getBootProperties();
        long connectionToProviderTimeout = getServerConnectionTimeout(bootP);
        long startConnectionToProviderTime = System.currentTimeMillis();
        long sleepWaitMillis = 10 * 1000;// 10 seconds as first interval
        X509Certificate[] cachedCertificatesChain = null;

        while (true) {
            SYSTEM_OUT.println("- Testing secure connection ... ");

            try {
                bizapp = this.testProvider(provider, cachedCertificatesChain);
                SYSTEM_OUT.println("- Connection successful.");

                AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig(bootP);
                CommonServerInteractor.INSTANCE.onKeystoreChange(keystoreConfig);
                return bizapp;
            } catch (AgentCallbackClientException exc) {

                // ...check if there's a SSL exception...
                if (exc.getExceptionOfType(SSLPeerUnverifiedException.class) != null) {
                    // note: we'll get "peer not authenticated" also for a timeout on the ssl socket(SSLSocketFactory).

                    String serverCertificateThumbprintPropValue =
                                bootP.getProperty("agent.setup.serverCertificateThumbprint");
                    if (StringUtils.isNotEmpty(serverCertificateThumbprintPropValue)) {
                        log.error(UNTRUSTED_CERTIFICATE_MSG, exc);
                        throw new AutoQuestionException(UNTRUSTED_CERTIFICATE_MSG);
                    }
                }

                SYSTEM_ERR.println("- Connection failed.");
                // safety so we won't have endless loop if properties file contains bad host/port.
                if (isAutoHostOrPort(bootP)) {
                    throw new AutoQuestionException("Unable to connect to " + HQConstants.PRODUCT + ".");
                }

                if (startConnectionToProviderTime + connectionToProviderTimeout <= System.currentTimeMillis()) {
                    SYSTEM_ERR.println("Timeout. Quitting the connection attempts.");
                    return null;
                }

                SYSTEM_ERR.println("Server might be down (or wrong IP/port were used). Waiting for " +
                            String.valueOf(sleepWaitMillis / 1000) + " seconds before retrying.");

                try {
                    Thread.sleep(sleepWaitMillis);
                    sleepWaitMillis += (sleepWaitMillis / 2);
                } catch (InterruptedException ie) {
                }

                CachedCertificateException cachedCertExc = getCachedCertificateException(exc);
                if (cachedCertExc != null) { // avoid asking the user to approve an already approved certificate
                    cachedCertificatesChain = cachedCertExc.getCachedCertificatesChain();
                }

                // Try again
            } catch (UserQuestionException e) {
                if (!isAutoHostOrPort(bootP)) {
                    boolean isConnectAnotherAnswer =
                                askQuestionUtil.askYesNoQuestion("Do you want to connect to another server", false,
                                            null);
                    if (isConnectAnotherAnswer) { // if answer is yes
                        throw e; // so that we ask the user again for details
                    }

                    return null; // user has decide to quit setup.
                } else {
                    SYSTEM_ERR.println("- Connection failed.");
                    throw new AutoQuestionException("Unable to connect to " + HQConstants.PRODUCT + ".");
                }
            }
        }
    }

    private CachedCertificateException getCachedCertificateException(AgentCallbackClientException exc) {
        CachedCertificateException e = (CachedCertificateException) exc.
                    getExceptionOfType(CachedCertificateException.class);
        return e;
    }

    private static int getCpuCount()
        throws SigarException {
        Sigar sigar = new Sigar();
        try {
            return sigar.getCpuInfoList().length;
        } finally {
            sigar.close();
        }
    }

    public static String getAgentName() {
        return AppdefEntityConstants.HQ_AGENT_PROTOTYPE_NAME + HQConstants.RESOURCE_NAME_DELIM
                    + GenericPlugin.getPlatformName();
    }

    private void cmdSetupIfNoProvider()
        throws AgentConnectionException, AgentRemoteException,
        IOException, AutoQuestionException {

        Properties bootProps = this.config.getBootProperties();
        int timeout = getStartupTimeout(bootProps);

        // Sleep until the agent is started
        this.cmdPing(timeout / 1000);

        // Prompt the agent to setup if the provider info is not specified.
        ProviderInfo providerInfo = this.camCommands.getProviderInfo();

        if (providerInfo == null) {
            this.cmdSetup();
        } else {
            setPlatformVmUniqueIdentifierInGuestinfo(providerInfo.getAgentToken());
        }
    }

    private void cmdSetup()
        throws AgentConnectionException, AgentRemoteException, IOException,
        AutoQuestionException {
        BizappCallbackClient bizapp = null;
        ProviderInfo providerInfo = this.camCommands.getProviderInfo();
        String provider = null, host = null, agentToken = null;
        Properties bootP;
        int sslConnectionPort = Integer.MIN_VALUE;

        bootP = this.config.getBootProperties();
        try {
            this.cmdPing(1);
        } catch (AgentConnectionException exc) {
            SYSTEM_ERR.println("Unable to setup agent: " + exc.getMessage());
            SYSTEM_ERR.println("The agent must be running prior to running " +
                        SETUP);
            return;
        }

        if (System.getenv("SHELL") != null && System.getProperty("os.name") != null &&
                    System.getProperty("os.name").toLowerCase().indexOf("win") > -1
                    && config.getBootProperty(AgentConfig.QPROP_PWORD) == null) {
            SYSTEM_ERR.println("You cannot use a shell command to register an agent in Windows");
            return;
        }

        SYSTEM_OUT.println("[ Running agent setup ]");

        // Load platform token (asks a question in case of error)
        PlatformToken platformToken = loadPlatfromToken(bootP, TOKEN_LOAD_ATTEMPTS_LIMIT);
        if (null == platformToken) {
            // We cannot proceed if we don't have a token for use
            String errMsg =
                        "You have exceeded the number of attempts permitted to access the default token location. Exiting agent setup.";
            log.error(errMsg);
            SYSTEM_ERR.println(errMsg);
            return;
        }

        boolean isReissueCertificateRequest = isReissueCertificateRequest(providerInfo, platformToken);
        agentToken = getEffectiveAgentToken(isReissueCertificateRequest, providerInfo, platformToken);
        if (!isSetupFlowWithKnownToken(providerInfo, platformToken)) {
            SYSTEM_ERR.println("The setup command cannot be used because the token file is deleted or damaged."
                        + " To run setup, correct the token file or reinstall the agent.");
            log.error("The epops-token file doesn't contain the expected token '" + providerInfo.getAgentToken()
                        + "' so the setup command cannot proceed.");
            return;
        }
        if (platformToken.wasTokenGeneratedByCurrentRun()) {
            SYSTEM_OUT.println("- The agent generated the following token");
        } else {
            SYSTEM_OUT.println("- The agent loaded an existing token");
        }
        SYSTEM_OUT.println("    " + agentToken);

        while (provider == null) {
            /*
             * Get host/ip + port from user/config
             */
            if (isReissueCertificateRequest) {
                host = getCurrentProviderHost(providerInfo);
                sslConnectionPort = getDefaultPort(providerInfo);
                SYSTEM_OUT.println("- The agent is already setup for " + HQConstants.PRODUCT + "@" +
                            host + ":" + sslConnectionPort);
            } else { // Regular setup
                int questionNumber = 1;
                while (true) {
                    try {
                        if (questionNumber == 1) {
                            host =
                                        askQuestionUtil.askQuestion("Enter the " + HQConstants.PRODUCT
                                                    + " hostname or IP address",
                                                    null,
                                                    AgentConfig.QPROP_IPADDR);

                            ProviderInfo.validateAddress(host);
                            questionNumber++;
                        }
                        if (questionNumber == 2) {
                            sslConnectionPort = askQuestionUtil.askIntQuestion("Enter the " + HQConstants.PRODUCT +
                                        " SSL port",
                                        HQConstants.DEFAULT_SSL_PORT,
                                        AgentConfig.QPROP_SSLPORT);
                            PropertiesUtil.validatePort(sslConnectionPort);
                            questionNumber++;
                        }
                    } catch (IllegalArgumentException illegalInputException) {
                        // If values were fetched from a file
                        boolean errorInFirstAutoQuestion =
                                    (questionNumber == 1)
                                                && (StringUtils.isNotBlank(bootP.getProperty(AgentConfig.QPROP_IPADDR)));
                        boolean errorInSecondAutoQuestion =
                                    (questionNumber == 2)
                                                && (StringUtils.isNotBlank(bootP.getProperty(AgentConfig.QPROP_SSLPORT)));
                        if (errorInFirstAutoQuestion || errorInSecondAutoQuestion) {
                            throw new AutoQuestionException(INVALID_ENTRY_AUTO_MESSAGE);
                        }
                        SYSTEM_OUT.println("The data you entered is incorrect.");
                        continue;
                    }
                    break;
                }
            }

            /*
             * Test the connection - serverInfo
             */
            // in this stage the agent still does not have a certificate.
            // append REGISTRATION_URL_SUFFIX to bizapp provider URL so that we'll
            // address the location that does not requires certificates.
            // do NOT modify provider value, it is used later when creating registeredProviderInfo
            // (and registeredProviderInfo should address the defaultProviderURL)
            provider = AgentCallbackClient.getDefaultProviderURL(host,
                        sslConnectionPort);

            try {
                bizapp = testConnectionToProvider(provider + REGISTRATION_URL_SUFFIX);
                if (bizapp == null) {
                    return;// user has decide to quit setup.
                }
            } catch (UserQuestionException e) {
                // safety so we won't have endless loop if getCurrentProviderHost returns bad/null host/port.
                if (isReissueCertificateRequest) {
                    throw new AutoQuestionException(
                                "Could not request a new certificate. Current provider host/port is invalid.");
                }
                provider = null;// reset it so we'll ask again for host,port
            } catch (IllegalArgumentException illegalArgsExc) {
                // This should not happen, as input has been validated before
                // In any case it's fatal at this point
                SYSTEM_ERR.println("Invalid argument: " + illegalArgsExc.getMessage());
                throw new AutoQuestionException(INVALID_ENTRY_AUTO_MESSAGE);
            }
        }

        /*
         * Generate CSR.
         */
        byte[] csr;
        KeyPair agentKeyPair;

        try {
            agentKeyPair = CertificateService.generateKeyPair();
            csr = CertificateService.generateCSR(agentToken, agentKeyPair);
        } catch (GeneralSecurityException exc) {
            SYSTEM_ERR.println("Unable to generate a certificate signing request: " +
                        exc.getMessage());
            return;
        } catch (OperatorCreationException exc) {
            SYSTEM_ERR.println("Unable to generate a certificate signing request: " +
                        exc.getMessage());
            return;
        }

        String signedCertificateStr = registerAgent(isReissueCertificateRequest, bizapp, csr, bootP);
        if (signedCertificateStr == null) {
            return;// there was a problem with the registration. don't continue.
        }

        /*
         * Post registration. Notify agent of a keystore change, validate returned values from server are properly
         * stored
         */
        try {
            X509Certificate certificate =
                        CertificateService.convertPemEncodedCertificateToX509Certificate(signedCertificateStr);
            validateCertificate(certificate, agentToken, agentKeyPair);
            importClientCertificateToKeystore(certificate, agentKeyPair);
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
            SYSTEM_ERR.println("- Error with the received certificate: " + exc.getMessage());
            return;
        }

        // Notify AgentDaemon of a keystore change, so that existing connections will load an updated reusable client
        this.camCommands.notifyKeystoreChanged();

        SYSTEM_OUT.println("- The agent has received a client certificate from " + HQConstants.PRODUCT
                    + ".");

        ProviderInfo registeredProviderInfo = new ProviderInfo(provider, agentToken, sslConnectionPort);
        this.camCommands.setProviderInfo(registeredProviderInfo);

        // Validating provider info
        providerInfo = this.camCommands.getProviderInfo();
        if (providerInfo == null ||
                    providerInfo.getProviderAddress().equals(provider) == false ||
                    providerInfo.getAgentToken().equals(agentToken) == false) {
            if (providerInfo == null) {
                SYSTEM_ERR.println(" - Unable to store " +
                            "" + HQConstants.PRODUCT + " information.");
            } else {
                SYSTEM_ERR.println("- Unable to validate stored server information; the agent is using " +
                            HQConstants.PRODUCT + " '" +
                            providerInfo.getProviderAddress() +
                            "' with token '" +
                            providerInfo.getAgentToken() + "'.");
            }
        } else {
            if (isReissueCertificateRequest) {
                SYSTEM_OUT.println("- The agent has successfully acquired a new certificate.");
            } else {
                SYSTEM_OUT.println("- The agent has been successfully registered.");
            }
        }
        setPlatformVmUniqueIdentifierInGuestinfo(providerInfo.getAgentToken());
        redirectOutputs(bootP); // win32
    }

    /*
     * Get credentials from user and register. Validate and store signed certificate.
     */
    private String registerAgent(boolean isReissueCertificateRequest,
                                 BizappCallbackClient bizapp,
                                 byte[] csr,
                                 Properties bootP)
        throws IOException, AutoQuestionException {
        String user, pword;
        String agentIP = getAgentName();
        RegisterAgentResult result;

        for (int attempts = 1; attempts <= LOGIN_ATTEMPTS_LIMIT; attempts++) {

            user = askQuestionUtil.askQuestion("Enter your " + HQConstants.PRODUCT +
                        " username", null, AgentConfig.QPROP_LOGIN);
            pword = askQuestionUtil.askQuestion("Enter your " + HQConstants.PRODUCT +
                        " password", null, true, false, AgentConfig.QPROP_PWORD);

            try {
                if (isReissueCertificateRequest) {
                    SYSTEM_OUT.println("- Sending request for a new certificate");
                } else {
                    SYSTEM_OUT.println("- Registering the agent with " + HQConstants.PRODUCT + ".");
                }

                result = bizapp.registerAgent(user, pword, agentIP, ProductProperties.getVersion(), getCpuCount(),
                            csr);

                if (StringUtils.isEmpty(result.errorMessage)) {
                    return result.certificate;
                }
            } catch (AgentCallbackClientException e) {
                SYSTEM_ERR.println("- Unable to register the agent due to server error.");
                log.error(e.getMessage(), e);
                return null;
            } catch (Exception exc) {
                log.error(exc.getMessage(), exc);
                if (isReissueCertificateRequest) {
                    SYSTEM_ERR.println("- Error while requesting a new certificate: " + exc.getMessage());
                } else {
                    SYSTEM_ERR.println("- Error registering the agent: " + exc.getMessage());
                }
                return null;
            }

            throwAutoQuestionExceptionIfPropertiesUsed(bootP, result.errorMessage);
            SYSTEM_ERR.println("- Unable to register the agent: " + result.errorMessage);
        }
        return null;
    }

    /**
     * Loads platform token from disk, from a user defined location, or from a default one if not defined.
     * 
     * If PlatformToken was not created (method returns a null), it means that we have passed the limit and haven't
     * succeeded loading the token.
     * 
     * @param bootProperties
     * @param maximumRetriesNumber
     * @return
     * @throws AutoQuestionException
     * @throws IOException
     */
    private PlatformToken loadPlatfromToken(Properties bootProperties,
                                            int maximumRetriesNumber)
        throws AutoQuestionException, IOException {
        PlatformToken platformToken = null;
        String currentTokenLinuxPath = bootProperties.getProperty(AgentConfig.QPROP_TOKEN_FILE_LINUX);
        String currentTokenWindowsPath = bootProperties.getProperty(AgentConfig.QPROP_TOKEN_FILE_WINDOWS);
        String customTokenPath = Os.isWindowsFamily() ? currentTokenWindowsPath : currentTokenLinuxPath;

        int retryNumber = 0;

        while (null == platformToken) {
            try {
                platformToken = new PlatformToken(currentTokenLinuxPath, currentTokenWindowsPath);
            } catch (TokenNotFoundException tokenEx) {
                log.error("Error loading platform token from file. ", tokenEx);
                if (isTokenPathConfiguredForAutomatedSetup(bootProperties)) {
                    throw new AutoQuestionException("Error loading platform token from file. " + tokenEx.getMessage());
                } else {
                    retryNumber++;
                    SYSTEM_OUT.println(tokenEx.getMessage());
                    if (retryNumber > maximumRetriesNumber) {
                        return null;
                    }

                    customTokenPath =
                                askQuestionUtil.askQuestionAcceptEmptyAnswer(
                                            "Correct the problem and press Enter to retry, or provide an alternative token path",
                                            customTokenPath,
                                            null);
                    if (!StringUtil.isNullOrEmpty(customTokenPath)) {
                        if (Os.isWindowsFamily()) {
                            currentTokenWindowsPath = customTokenPath;
                        } else {
                            currentTokenLinuxPath = customTokenPath;
                        }
                    }
                }
            }
        }
        return platformToken;
    }

    private boolean isAutoHostOrPort(Properties bootP) {
        return bootP.getProperty(AgentConfig.QPROP_IPADDR) != null ||
                    bootP.getProperty(AgentConfig.QPROP_SSLPORT) != null;
    }

    private boolean isTokenPathConfiguredForAutomatedSetup(Properties bootProperties) {
        if (Os.isWindowsFamily()) {
            return (null != bootProperties.get(AgentConfig.QPROP_TOKEN_FILE_WINDOWS));
        }
        return (null != bootProperties.get(AgentConfig.QPROP_TOKEN_FILE_LINUX));
    }

    /**
     * Check to see if this agent already has a setup for a server. If it does, allow the user to re-setup with the
     * current token. This will trigger a new CSR request. Can be used if current cert. is about to expire.
     */
    private boolean isReissueCertificateRequest(ProviderInfo providerInfo,
                                                PlatformToken platformTokenManager) {
        if (platformTokenManager.wasTokenGeneratedByCurrentRun()) {
            return false;
        }
        if (StringUtils.isEmpty(platformTokenManager.getValue())) {
            return false;
        }
        if (providerInfo == null) {
            return false;
        }
        if (!platformTokenManager.getValue().equals(providerInfo.getAgentToken())) {
            return false;
        }
        // PlatfromToken is not empty and equals the stored Agent Token (which is in turn not empty)
        return true;
    }

    /**
     * Check to see if this agent already has a setup for a server, but with a token different than read from file or
     * generated. This flow is currently unsupported
     */
    private boolean isSetupFlowWithKnownToken(ProviderInfo providerInfo,
                                              PlatformToken platformTokenManager) {
        if (StringUtils.isNotEmpty(platformTokenManager.getValue())
                    && providerInfo != null && StringUtils.isNotEmpty(providerInfo.getAgentToken())
                    && !platformTokenManager.getValue().equals(providerInfo.getAgentToken())) {
            return false;
        }

        // Setup can continue
        return true;
    }

    private String getCurrentProviderHost(final ProviderInfo providerInfo) {
        if (providerInfo != null) {
            return providerInfo.getProviderHost();
        }
        return null;
    }

    private int getDefaultPort(final ProviderInfo providerInfo) {
        if (providerInfo != null) {
            return providerInfo.getProviderPort();
        }
        return HQConstants.DEFAULT_SSL_PORT;// Default
    }

    private String getEffectiveAgentToken(boolean isReissueCertificateRequest,
                                          final ProviderInfo providerInfo,
                                          PlatformToken platformTokenManager) {
        if (!isReissueCertificateRequest) {
            return platformTokenManager.getValue();
        }
        // In case this is a re-issue request
        String currentAgentToken = null;
        if (providerInfo != null) {
            currentAgentToken = providerInfo.getAgentToken();
        }
        return currentAgentToken;
    }

    private void throwAutoQuestionExceptionIfPropertiesUsed(Properties bootP,
                                                            String errorMessage)
        throws AutoQuestionException {
        if (bootP.getProperty(AgentConfig.QPROP_LOGIN) != null || bootP.getProperty(AgentConfig.QPROP_PWORD) != null) {
            throw new AutoQuestionException(errorMessage);
        }
    }

    private void validateCertificate(X509Certificate certificate,
                                     String agentTokenInCsr,
                                     KeyPair agentKeyPair)
        throws CertificateException {
        String agentTokenFromCert = CertificateService.extractAgentTokenFromCertificate(certificate);
        if (!agentTokenInCsr.equals(agentTokenFromCert) || !agentKeyPair.getPublic().equals(certificate.getPublicKey())) {
            throw new CertificateException(
                        "The certificate received from the server does not match the request.");
        }
    }

    private void importClientCertificateToKeystore(X509Certificate certificate,
                                                   KeyPair agentKeyPair)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig(this.config);
        CertificateService.saveCertificateToKeystore(keystoreConfig.getClientCertificateAlias(), certificate,
                    agentKeyPair, keystoreConfig);
        log.info("Imported client certificate into keystore");
    }

    private void verifyAgentRunning(ServerSocket startupSock)
        throws AgentInvokeException
    {
        DataInputStream dIs = null;
        Socket conn = null;
        try {

            conn = startupSock.accept();

            dIs = new DataInputStream(conn.getInputStream());

            if (dIs.readInt() != 1) {
                throw new AgentInvokeException("Agent reported an error " +
                            "while starting up");
            }

        } catch (InterruptedIOException exc) {
            throw new AgentInvokeException("Timed out waiting for Agent " +
                        "to report startup success");
        } catch (IOException exc) {
            throw new AgentInvokeException("Agent failure while starting");
        } finally {
            if (startupSock != null) {
                try {
                    startupSock.close();
                } catch (IOException exc) {
                }
            }
            if (dIs != null) {
                try {
                    dIs.close();
                } catch (IOException exc) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException exc) {
                }
            }
        }

        try {
            this.agtCommands.ping();
        } catch (Exception exc) {
            throw new AgentInvokeException("Unable to ping agent: " +
                        exc.getMessage());
        }
    }

    private PrintStream newLogStream(String stream,
                                     Properties bootProps)
        throws AgentConfigException, IOException {
        Logger logger = Logger.getLogger(stream);
        Level level = Level.toLevel(bootProps.getProperty("agent.startup.logLevel." + stream,
                    bootProps.getProperty("agent.logLevel." + stream,
                                DEFAULT_LOG_LEVEL)));
        PatternLayout layout = new PatternLayout(bootProps.getProperty("agent.startup.ConversionPattern",
                    LOG_PATTERN_LAYOUT));
        RollingFileAppender fileAppender = new RollingFileAppender(layout, getStartupLogFile(bootProps), true);
        fileAppender.setImmediateFlush(true);
        fileAppender.setBufferedIO(false);
        fileAppender.setBufferSize(BUFFER_SIZE);
        fileAppender.setMaxFileSize(bootProps.getProperty("agent.startup.MaxFileSize", MAX_FILE_SIZE));
        fileAppender.setMaxBackupIndex(
                    PropertiesUtil.getGreatOrEqualZeroIntValue("agent.startup.MaxBackupIndex",
                                bootProps.getProperty("agent.startup.MaxBackupIndex"), MAX_FILES));
        logger.addAppender(fileAppender);
        logger.setAdditivity(false);
        return new PrintStream(new LoggingOutputStream(logger, level), true);
    }

    private void redirectOutputs(Properties bootProp) {
        if (this.redirectedOutputs) {
            return;
        }
        this.redirectedOutputs = true;

        try {
            System.setErr(newLogStream("SystemErr", bootProp));
            System.setOut(newLogStream("SystemOut", bootProp));
        } catch (Exception e) {
            e.printStackTrace(SYSTEM_ERR);
        }
    }

    public static Thread getAgentDaemonThread() {
        return agentDaemonThread;
    }

    private int cmdStart(boolean force)
        throws AgentInvokeException
    {
        ServerSocket startupSock = null;
        ProviderInfo providerInfo;
        Properties bootProps;

        // This should be the only place that initializes the keystore.
        // All consumers of the keystore should wait for it to be initialized.
        initializeKeystoreBeforeUsage();

        // Try to ping the agent one time to see if the agent is already up
        try {
            this.cmdPing(1);
            SYSTEM_OUT.println("Agent already running");
            return -1;
        } catch (AgentConnectionException exc) {
            // Normal operation
        } catch (AgentRemoteException exc) {
            // Very nearly a normal operation
        }

        bootProps = this.config.getBootProperties();

        try {
            int iSleepTime = getStartupTimeout(bootProps);

            startupSock = new ServerSocket(0);
            startupSock.setSoTimeout(iSleepTime);
        } catch (IOException e) {
            if (startupSock != null) {
                try {
                    startupSock.close();
                } catch (IOException e1) {
                }
            }
            AgentInvokeException ex =
                        new AgentInvokeException("Unable to setup a socket to listen for Agent startup: " + e);
            ex.initCause(e);
            throw ex;
        }

        SYSTEM_OUT.println("- Invoking agent");

        int localPort = startupSock.getLocalPort();
        try {
            this.config.setNotifyUpPort(localPort);
        } catch (AgentConfigException e) {
            try {
                startupSock.close();
            } catch (IOException exc) {
            }
            throw new AgentInvokeException("Invalid notify up port: " + localPort);
        }

        RunnableAgent runnableAgent = new AgentDaemon.RunnableAgent(this.config);
        agentDaemonThread = new Thread(runnableAgent);
        agentDaemonThread.setName("AgentDaemonMain");
        AgentUpgradeManager.setAgentDaemonThread(agentDaemonThread);
        AgentUpgradeManager.setAgent(runnableAgent);
        agentDaemonThread.setDaemon(true);
        agentDaemonThread.start();
        SYSTEM_OUT.println("- Agent thread running");

        /* Now comes the painful task of figuring out if the agent started correctly. */
        SYSTEM_OUT.println("- Verifying if agent is running...");
        // startupSock is closed internally
        this.verifyAgentRunning(startupSock);
        SYSTEM_OUT.println("- Agent is running");

        // Ask the agent if they have a server setup
        try {
            providerInfo = this.camCommands.getProviderInfo();
        } catch (Exception exc) {
            // This should rarely (never) occur, since we just ensured things
            // were operational.
            throw new AgentInvokeException("Unexpected connection exception: " +
                        "agent is still running");
        }

        SYSTEM_OUT.println("Agent successfully started");

        // Only force a setup if we are not running the agent in Java Service Wrapper mode
        if (providerInfo == null && !WrapperManager.isControlledByNativeWrapper()) {
            SYSTEM_OUT.println();
            return FORCE_SETUP;
        } else {
            redirectOutputs(bootProps); // win32
            return 0;
        }

    }// cmdStart

    private void initializeKeystoreBeforeUsage()
        throws AgentInvokeException {
        KeystoreConfig keystoreConfig = new AgentKeystoreConfig(this.config);
        try {
            log.debug("Initializing keystore before its usage");
            KeystoreManager.getKeystoreManager().initializeKeyStore(keystoreConfig);
        } catch (KeyStoreException exc) {
            throw new AgentInvokeException("Failed to initialize keystore: " + exc);
        } catch (IOException exc) {
            throw new AgentInvokeException("Failed to initialize keystore: " + exc);
        }
    }

    private static void cmdSetProp(String propKey,
                                   String propVal)
        throws AgentConfigException {
        final String propFile = System.getProperty(AgentConfig.PROP_PROPFILE, AgentConfig.DEFAULT_PROPFILE);
        AgentConfig.setDefaultProps(propFile);
        AgentConfig.ensurePropertiesEncryption(propFile);

        try {
            String propEncKey = PropertyEncryptionUtil.getPropertyEncryptionKey(AgentConfig.PROP_ENC_KEY_FILE[1]);
            Map<String, String> entriesToStore = new HashMap<String, String>();
            entriesToStore.put(propKey, propVal);
            PropertyUtil.storeProperties(propFile, propEncKey, entriesToStore);
        } catch (Exception exc) {
            throw new AgentConfigException(exc);
        }
    }

    private String getStartupLogFile(Properties bootProps)
        throws AgentConfigException {
        String logFile;

        if ((logFile = bootProps.getProperty(PROP_LOGFILE)) == null) {
            throw new AgentConfigException(PROP_LOGFILE + " is undefined");
        }

        return logFile + ".startup";
    }

    /**
     * returns the startup timeout in milliseconds
     * 
     * @param bootProps
     * @return number of milliseconds for ServerConnectionTimeout
     */
    private static int getStartupTimeout(Properties bootProps) {
        String sleepTime = bootProps.getProperty(PROP_STARTUP_TIMEOUT);
        return PropertiesUtil.getGreatOrEqualZeroIntValue(PROP_STARTUP_TIMEOUT, sleepTime, AGENT_STARTUP_TIMEOUT) * 1000;
    }

    /**
     * returns the server connection timeout in milliseconds
     * 
     * @param bootProps
     * @return number of milliseconds for ServerConnectionTimeout
     */
    private static int getServerConnectionTimeout(Properties bootProps) {
        String sleepTime = bootProps.getProperty(PROP_SERVER_CONNECTION_TIMEOUT);
        return PropertiesUtil.getGreatOrEqualZeroIntValue(PROP_SERVER_CONNECTION_TIMEOUT, sleepTime,
                    SERVER_CONNECTION_TIMEOUT_DEFAULT) * 1000;
    }

    private static int getUseTime(String val) {
        return PropertiesUtil.getGreatOrEqualZeroIntValue("UseTime", val, 1);
    }

    private static class LoggerSettingsFileWatcher extends FileWatcher {
        private final String propFile;

        public LoggerSettingsFileWatcher(Sigar sigar,
                                         String propertiesFile) {
            super(sigar);
            propFile = propertiesFile;

            File file = AgentConfig.getPropertyFile(propFile);
            try {
                add(file);
            } catch (SigarException e) { // Sigar filewatcher tries load the agent's properties file for log level
                                         // information
                // Fails in case installed on a locallized path.
                SYSTEM_OUT.println("The log level is set to default. If you customized the log level, "
                            + "you must restart the agent after the registration process finishes in order "
                            + "to apply the changes.");
            }
            setInterval(60000);
        }

        @Override
        public void onChange(FileInfo fileInfo) {
            try {
                SYSTEM_OUT.println("A change has been detected in " + fileInfo.getName()
                            + ", Reloading the logging configuration.");
                PropertyConfigurator.configure(AgentConfig.getProperties(propFile));
            } catch (AgentConfigException e) {
                SYSTEM_ERR.println("Error reloading the logging configuration: " + e.getMessage() + ".");
                e.printStackTrace(SYSTEM_ERR);
            }
        }
    }

    /**
     * Initialize the AgentClient
     * 
     * @param generateToken If set to true, generate the agent token, otherwise wait until the tokens are available.
     * 
     * @return An initialized AgentClient
     */
    private static AgentClient initializeAgent(boolean generateToken)
        throws AgentConfigException {

        final String propFile = System.getProperty(AgentConfig.PROP_PROPFILE, AgentConfig.DEFAULT_PROPFILE);
        AgentConfig.setDefaultProps(propFile);
        AgentConfig.ensurePropertiesEncryption(propFile);

        SecureAgentConnection conn;
        AgentConfig cfg;
        String authToken;

        // console appender until we have configured logging.
        BasicConfigurator.configure();

        try {
            cfg = AgentConfig.newInstance(propFile, true);
        } catch (IOException exc) {
            SYSTEM_ERR.println("Error: " + exc);
            return null;
        } catch (AgentConfigException exc) {
            SYSTEM_ERR.println("Agent properties error: " + exc.getMessage());
            return null;
        }

        // we wait until AgentConfig.newInstance has merged
        // all properties to configure logging.
        Properties bootProps = cfg.getBootProperties();
        if (!checkCanWriteToLog(bootProps)) {
            return null;
        }
        PropertyConfigurator.configure(bootProps);

        FileWatcherThread watcherThread = FileWatcherThread.getInstance();
        FileWatcher loggingWatcher = new LoggerSettingsFileWatcher(new Sigar(), propFile);
        watcherThread.add(loggingWatcher);
        watcherThread.doStart();

        AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig(cfg);
        String tokenFile = cfg.getTokenFile();
        if (generateToken) {
            try {
                authToken = AgentClientUtil.getLocalAuthToken(tokenFile);
            } catch (FileNotFoundException exc) {
                SYSTEM_ERR.print("- Unable to load the agent token file.  Generating" +
                            " a new one ... ");
                try {
                    String nToken = SecurityUtil.generateRandomToken();

                    AgentClientUtil.generateNewTokenFile(tokenFile, nToken);
                    authToken = AgentClientUtil.getLocalAuthToken(tokenFile);
                } catch (IOException oexc) {
                    SYSTEM_ERR.println("Unable to setup the preliminary agent auth " +
                                "tokens: " + exc.getMessage());
                    return null;
                }
                SYSTEM_ERR.println("Done");
            } catch (IOException exc) {
                SYSTEM_ERR.println("Unable to obtain the necessary authentication tokens" +
                            " that are required to talk to the agent: " + exc.getMessage());
                return null;
            }
            conn = new SecureAgentConnection(cfg.getListenIp(), cfg.getListenPort(), authToken, keystoreConfig,
                        keystoreConfig.isAcceptUnverifiedCert());
            // TODO need to figure out where the connection should be closed AND close it! }:^(

            return new AgentClient(cfg, conn);

        } else {
            // Not the main agent daemon process, wait for the token to become
            // available. We will only wait up to the configured agent.startupTimeOut
            long initializeStartTime = System.currentTimeMillis();
            long startupTimeout = getStartupTimeout(bootProps);
            while (initializeStartTime > (System.currentTimeMillis() - startupTimeout)) {
                try {
                    authToken = AgentClientUtil.getLocalAuthToken(tokenFile);
                    conn = new SecureAgentConnection(cfg.getListenIp(), cfg.getListenPort(), authToken,
                                keystoreConfig, keystoreConfig.isAcceptUnverifiedCert());
                    // TODO need to figure out where the connection should be closed AND close it! }:^(

                    return new AgentClient(cfg, conn);
                } catch (FileNotFoundException exc) {
                    SYSTEM_ERR.println("- No token file found, Waiting for " +
                                "the agent to initialize");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        SYSTEM_ERR.println("The process was interrupted! Shutting down.");
                        return null;
                    }
                } catch (IOException e) {
                    SYSTEM_ERR.println("Unable to read the preliminary agent auth " +
                                "tokens, Waiting for the agent to initialize " +
                                "(error was: " + e.getMessage() + ")");
                }
            }
            SYSTEM_ERR.println("Timeout waiting for token file");
            return null;
        }
    }

    public static void main(String args[]) {

        if (args.length == 3 && args[0].equals(SET_PROPERTY)) {
            try {
                cmdSetProp(args[1], args[2]);
            } catch (AgentConfigException e) {
                SYSTEM_ERR.println("Error: " + e.getMessage());
                e.printStackTrace(SYSTEM_ERR);
            }
            return;
        }

        if (args.length < 1 ||
                    !(args[0].equals(PING) ||
                                args[0].equals(DIE) ||
                                args[0].equals(START) ||
                                args[0].equals(STATUS) ||
                                args[0].equals(RESTART) ||
                                args[0].equals(SETUP) ||
                    args[0].equals(SETUP_IF_NO_PROVIDER)))
        {
            SYSTEM_ERR.println("Syntax: program " +
                        "<" + PING + " [numAttempts] | " + DIE + " [dieTime] | " + START +
                        " | " + STATUS + " | " + RESTART + " | " + SETUP +
                        " | " + SETUP_IF_NO_PROVIDER + " | " + SET_PROPERTY + " >");
            return;
        }

        AgentClient client;
        try {
            if (args[0].equals(START)) {
                // Only generate tokens on agent startup.
                client = initializeAgent(true);
            } else {
                client = initializeAgent(false);
            }

            if (client == null) {
                return;
            }

            int nWait;

            if (args[0].equals(PING)) {
                if (args.length == 3) {
                    nWait = getUseTime(args[2]);
                } else {
                    nWait = 1;
                }
                client.cmdPing(nWait);
            } else if (args[0].equals(DIE)) {
                if (args.length == 2) {
                    nWait = getUseTime(args[1]);
                } else {
                    nWait = 1;
                }
                SYSTEM_OUT.println("Stopping agent ... ");
                try {
                    client.cmdDie(nWait);
                    SYSTEM_OUT.println("Success -- agent is stopped!");
                } catch (Exception exc) {
                    SYSTEM_OUT.println("Failed to stop agent: " +
                                exc.getMessage());
                }
            } else if (args[0].equals(START)) {
                int errVal = client.cmdStart(false);
                if (errVal == FORCE_SETUP) {
                    errVal = 0;
                    client.cmdSetupIfNoProvider();
                }
            } else if (args[0].equals(STATUS)) {
                client.cmdStatus();
            } else if (args[0].equals(SETUP)) {
                client.cmdSetup();
            } else if (args[0].equals(SETUP_IF_NO_PROVIDER)) {
                client.cmdSetupIfNoProvider();
            } else if (args[0].equals(RESTART)) {
                client.cmdRestart();
            } else {
                throw new IllegalStateException("Unhandled condition");
            }
        } catch (AutoQuestionException exc) {
            SYSTEM_ERR.println("Unable to complete automatic agent setup. " +
                        exc.getMessage());
        } catch (AgentInvokeException exc) {
            SYSTEM_ERR.println("Error invoking the agent: " + exc.getMessage());
        } catch (AgentConnectionException exc) {
            SYSTEM_ERR.println("Error contacting the agent: " + exc.getMessage());
        } catch (AgentRemoteException exc) {
            SYSTEM_ERR.println("Error executing the remote method: " +
                        exc.getMessage());
        } catch (IllegalStateException exc) {
            SYSTEM_ERR.println("The agent is in an illegal state: " + exc.getMessage());
        } catch (Exception exc) {
            SYSTEM_ERR.println("Error: " + exc.getMessage());
            exc.printStackTrace(SYSTEM_ERR);
        }
    }

    private static boolean checkCanWriteToLog(Properties props) {

        String logFileName = props.getProperty("agent.logFile");
        if (logFileName == null) {
            SYSTEM_ERR.println("agent.logFile is not set. "
                        + "\nCannot start the agent.");
            return false;
        }
        File logFile = new File(logFileName);

        File logDir = logFile.getParentFile();
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                SYSTEM_ERR.println("Log directory does not exist and "
                            + "could not be created: "
                            + logDir.getAbsolutePath()
                            + "\nCannot start HQ agent.");
                return false;
            }
        }
        if (!logDir.canWrite()) {
            SYSTEM_ERR.println("Cannot write to the log directory: "
                        + logDir.getAbsolutePath()
                        + "\nEnsure this directory is owned by the user '"
                        + System.getProperty("user.name") + "' and is "
                        + "not a read-only directory."
                        + "\nCannot start the agent.");
            return false;
        }
        if (logFile.exists() && !logFile.canWrite()) {
            SYSTEM_ERR.println("Cannot write to the log file: "
                        + logFile.getAbsolutePath()
                        + "\nEnsure this file is owned by the user '"
                        + System.getProperty("user.name") + "' and is "
                        + "not a read-only file."
                        + "\nCannot start the agent.");
            return false;
        }

        return true;
    }

    /**
     * if this platform is a VMware Guest, set its VM_UNIQUE_IDENTIFIER_ENV_KEY_NAME in machine Guestinfo. this value
     * will use us to make the VM<->OS connection in the adapter. content is the agent token
     * 
     * @param agentToken
     */
    private void setPlatformVmUniqueIdentifierInGuestinfo(String agentToken) {
        VMwareGuestInfo.setGuestInfoValue(VM_UNIQUE_IDENTIFIER_ENV_KEY_NAME, agentToken);
    }
}
