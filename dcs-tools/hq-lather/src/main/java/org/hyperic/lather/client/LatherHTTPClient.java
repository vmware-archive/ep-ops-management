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

package org.hyperic.lather.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;
import org.hyperic.util.encoding.Base64;
import org.hyperic.util.http.HttpConfig;
import org.hyperic.util.http.ServerHttpClient;

/**
 * The LatherClient is the base object which is used to invoke remote Lather methods.
 */
public class LatherHTTPClient implements LatherClient {
    public static final int TIMEOUT_CONN = 10 * 1000;
    public static final int TIMEOUT_DATA = 40 * 1000;

    public static final String HDR_ERROR = "X-error-response";
    public static final String HDR_VALUECLASS = "X-latherValue-class";
    public static final String CMD_AI_SEND_REPORT = "aiSendReport";
    public static final String CMD_AI_SEND_RUNTIME_REPORT = "aiSendRuntimeReport";
    private final ServerHttpClient client;
    private final LatherXCoder xCoder;
    private final String baseURL;

    public LatherHTTPClient(String baseURL)
        throws Exception {
        this(baseURL, TIMEOUT_CONN, TIMEOUT_DATA, null);
    }

    public LatherHTTPClient(String baseURL,
                            int timeoutConn,
                            int timeoutData,
                            AgentConfig agtCfg) {
        this(baseURL, createClient(timeoutConn, timeoutData, agtCfg));
    }

    public LatherHTTPClient(String baseURL,
                            ServerHttpClient client) {
        this.client = client;
        this.baseURL = baseURL;
        xCoder = new LatherXCoder();
    }

    private static ServerHttpClient createClient(int timeoutConn,
                                                 int timeoutData,
                                                 AgentConfig agtCfg) {
        ServerHttpClient client;
        try {
            client = new ServerHttpClient(new AgentKeystoreConfig(agtCfg),
                        getHttpConfig(timeoutConn, timeoutData), false,
                        getMaxRequestsPerConnection(),
                        isSupportRRDNS(), getFailPeriodInMin(), getDownPeriodInMin());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return client;
    }

    public static int getMaxRequestsPerConnection() {
        String maxRequestsPerConnection = AgentConfig.getDefaultProperties()
                    .getProperty(
                                AgentConfig.PROP_MAX_HTTP_REQUESTS_PER_CONNECTION[0]);
        try {
            return Integer.parseInt(maxRequestsPerConnection);
        } catch (NumberFormatException e) {
            return AgentConfig.MAX_HTTP_REQUESTS_PER_CONNECTION;
        }
    }

    public static HttpConfig getHttpConfig(int timeoutConn,
                                           int timeoutData) {
        String proxyHostname = System.getProperty("lather.proxyHost", null);
        int proxyPort = Integer.getInteger("lather.proxyPort", new Integer(-1))
                    .intValue();

        HttpConfig config = new HttpConfig();
        config.setConnectionTimeout(timeoutConn);
        config.setSocketTimeout(timeoutData);
        config.setProxyHostname(proxyHostname);
        config.setProxyPort(proxyPort);

        return config;
    }

    public LatherValue invoke(String method,
                              LatherValue args)
        throws IOException, LatherRemoteException {
        // Get close connection default behavior from agent properties. This
        // conversion is null safe.
        boolean closeConn = Boolean.parseBoolean(AgentConfig
                    .getDefaultProperties().getProperty(
                                AgentConfig.PROP_CLOSE_HTTP_CONNECTION_BY_DEFAULT[0]));

        return invoke(method, args, closeConn);
    }

    public LatherValue invoke(String method,
                              LatherValue args,
                              boolean closeConn)
        throws IOException, LatherRemoteException {
        HttpResponse response = invokeUnparsed(method, args, closeConn);

        return parseLatherHttpResponse(response);
    }

    public LatherValue parseLatherHttpResponse(HttpResponse response)
        throws IOException, LatherRemoteException {
        if ((response != null)
                    && (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
            ByteArrayInputStream bIs;
            DataInputStream dIs;
            Header errHeader = response.getFirstHeader(HDR_ERROR);
            Header clsHeader = response.getFirstHeader(HDR_VALUECLASS);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            if (errHeader != null) {
                throw new LatherRemoteException(responseBody);
            }

            if (clsHeader == null) {
                throw new IOException(
                            "Server returned malformed result: did not contain a value class header");
            }

            Class<?> resClass;

            try {
                resClass = Class.forName(clsHeader.getValue());
            } catch (ClassNotFoundException exc) {
                throw new LatherRemoteException("Server returned a class '"
                            + clsHeader.getValue()
                            + "' which the client did not have access to");
            }

            try {
                bIs = new ByteArrayInputStream(Base64.decode(responseBody));
            } catch (IllegalArgumentException e) {
                throw new SystemException(
                            "could not decode response from server body="
                                        + responseBody, e);
            }
            dIs = new DataInputStream(bIs);

            return xCoder.decode(dIs, resClass);
        } else {
            EntityUtils.consume(response.getEntity());
            throw new IOException("Connection failure: "
                        + response.getStatusLine());
        }
    }

    public HttpResponse invokeUnparsed(String method,
                                       LatherValue args,
                                       boolean closeConn)
        throws IOException, LatherRemoteException {
        ByteArrayOutputStream bOs = new ByteArrayOutputStream();
        DataOutputStream dOs = new DataOutputStream(bOs);

        xCoder.encode(args, dOs);

        byte[] rawData = bOs.toByteArray();
        String encodedArgs = Base64.encode(rawData);
        Map<String, String> postParams = new HashMap<String, String>();

        postParams.put("method", method);
        postParams.put("args", encodedArgs);
        postParams.put("argsClass", args.getClass().getName());
        shouldExpect100Continue(postParams);
        HttpResponse response = client.post(baseURL, postParams, closeConn);

        if ((response == null)
                    || (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)) {
            EntityUtils.consume(response.getEntity());
            throw new IOException("Connection failure: "
                        + response.getStatusLine());
        }

        return response;
    }

    //TODO Remove fix at 6.5- support expect 100-continue for AI reports
    private void shouldExpect100Continue(Map<String, String> postParams) {
       String method = postParams.get("method");
       if (method != null &&
          (CMD_AI_SEND_RUNTIME_REPORT.equals(method) || CMD_AI_SEND_REPORT.equals(method))){
           postParams.put(HttpHeaders.EXPECT, HTTP.EXPECT_CONTINUE);
       }

    }

    public static int getFailPeriodInMin() {
        return getIntConfig(
                    AgentConfig.PROP_COMMUNICATION_FAIL_PERIOD_IN_MINUTES[0],
                    AgentConfig.COMMUNICATION_FAIL_PERIOD_IN_MINUTES);
    }

    public static int getDownPeriodInMin() {
        return getIntConfig(
                    AgentConfig.PROP_COMMUNICATION_DOWN_PERIOD_IN_MINUTES[0],
                    AgentConfig.COMMUNICATION_DOWN_PERIOD_IN_MINUTES);
    }

    public static boolean isSupportRRDNS() {
        return getBooleanConfig(
                    AgentConfig.PROP_SUPPORT_RRDNS[0], AgentConfig.SUPPORT_RRDNS);
    }

    private static int getIntConfig(String propName,
                                    int defValue) {
        String value = AgentConfig.getDefaultProperties().getProperty(propName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static boolean getBooleanConfig(String propName,
                                            boolean defValue) {
        String value = AgentConfig.getDefaultProperties().getProperty(propName);
        return Boolean.parseBoolean(value);
    }

}
