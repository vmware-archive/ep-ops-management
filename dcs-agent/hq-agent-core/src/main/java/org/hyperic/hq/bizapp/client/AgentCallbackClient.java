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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.stats.AgentStatsCollector;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.client.common.CommonServerInteractor;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.client.LatherHTTPClient;
import org.hyperic.util.http.ServerHttpClient;

/**
 * Central place for communication back to the server.
 */
public abstract class AgentCallbackClient {
    private static final String HTTP_PROTOCOL = "https";
    private static final String LATHER_DEFAULT_PATH = "/epops-webapp/lather";
    private static final Log log = LogFactory.getLog(AgentCallbackClient.class);
    private static final String LATHER_CMD = "LATHER_CMD";
    private final ProviderFetcher fetcher; // Storage of provider info
    private static AgentStatsCollector statsCollector = AgentStatsCollector.getInstance();
    static {
        statsCollector.register(LATHER_CMD);
        for (String cmd : CommandInfo.ALL_COMMANDS) {
            statsCollector.register(LATHER_CMD + "_" + cmd.toUpperCase());
        }
    }
    private static final AtomicReference<AgentConfig> config = new AtomicReference<AgentConfig>();

    public static void setAgentConfig(AgentConfig cfg) {
        config.set(cfg);
    }

    public static AgentConfig getAgentConfig() {
        return config.get();
    }

    protected ServerHttpClient getReusableClient() {
        ServerHttpClient reusableClient = CommonServerInteractor.INSTANCE.getHttpClient();

        return reusableClient;
    }

    public AgentCallbackClient(ProviderFetcher fetcher,
                               AgentConfig config) {
        this.fetcher = fetcher;
        resetProvider();
        setAgentConfig(config);
    }

    void resetProvider() {
    }

    /**
     * Get the most up-to-date information about what our provider is, from the storage provider.
     * 
     * @return the string provider (such as jnp:stuff or http:otherstuff)
     */
    protected ProviderInfo getProvider()
        throws AgentCallbackClientException
    {
        ProviderInfo val = fetcher.getProvider();

        if (val == null) {
            final String msg = "Unable to communicate with server -- " +
                        "provider not yet setup. Agent might still be initializing.";
            throw new AgentCallbackClientException(msg);
        }
        return val;
    }

    /**
     * Generate a provider URL given a host and port. This routine adds in the prefix (such as http:, etc.) as well as
     * the URL after the host to identify the server interface (if necessary)
     * 
     * @param host Host to generate provider for
     * @param port Port to use for provider. If it is -1, the default port will be used.
     * @return default provider url, or null if such could not be constructed.
     */
    public static String getDefaultProviderURL(String host,
                                               int port) {
        if (port == -1) {
            port = HQConstants.DEFAULT_SSL_PORT;
        }

        try {
            return new URL(HTTP_PROTOCOL, host, port, LATHER_DEFAULT_PATH).toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Retrieve the host name from a provider URL.
     * 
     * @param providerURL The provider URL.
     * @return The host name.
     */
    public static String getHostFromProviderURL(String providerURL) {
        URL url = null;
        try {
            url = new URL(providerURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return url.getHost();
    }

    protected LatherValue invokeLatherCall(ProviderInfo provider,
                                           String methodName,
                                           LatherValue args)
        throws AgentCallbackClientException {
        // Get close connection default behavior from agent properties. This conversion is null safe.
        boolean closeConn =
                    Boolean.parseBoolean(
                                AgentConfig.getDefaultProperties().getProperty(
                                            AgentConfig.PROP_CLOSE_HTTP_CONNECTION_BY_DEFAULT[0]));

        String addr = provider.getProviderAddress();
        LatherHTTPClient latherClient = new LatherHTTPClient(addr, getReusableClient());
        return invokeLatherCall(provider, methodName, args, latherClient, closeConn);
    }

    protected LatherValue invokeLatherCall(ProviderInfo provider,
                                           String methodName,
                                           LatherValue args,
                                           LatherHTTPClient latherClient)
        throws AgentCallbackClientException {
        // Get close connection default behavior from agent properties. This conversion is null safe.
        boolean closeConn =
                    Boolean.parseBoolean(
                                AgentConfig.getDefaultProperties().getProperty(
                                            AgentConfig.PROP_CLOSE_HTTP_CONNECTION_BY_DEFAULT[0]));

        return invokeLatherCall(provider, methodName, args, latherClient, closeConn);
    }

    protected LatherValue invokeLatherCall(ProviderInfo provider,
                                           String methodName,
                                           LatherValue args,
                                           boolean closeConn)
        throws AgentCallbackClientException {
        String addr = provider.getProviderAddress();
        LatherHTTPClient latherClient = new LatherHTTPClient(addr, getReusableClient());
        return invokeLatherCall(provider, methodName, args, latherClient, closeConn);
    }

    protected LatherValue invokeLatherCall(ProviderInfo provider,
                                           String methodName,
                                           LatherValue args,
                                           LatherHTTPClient latherClient,
                                           boolean closeConn)
        throws AgentCallbackClientException {
        final boolean debug = log.isDebugEnabled();
        String addr = provider.getProviderAddress();

        try {
            final long start = now();
            LatherValue rtn = latherClient.invoke(methodName, args, closeConn);
            final long duration = now() - start;
            statsCollector.addStat(duration, LATHER_CMD);
            statsCollector.addStat(duration, LATHER_CMD + "_" + methodName.toUpperCase());
            return rtn;
        } catch (SSLException e) {
            if (debug) {
                log.debug(e, e);
            }
            throw new AgentCallbackClientException(e);
        } catch (ConnectException exc) {
            // All exceptions are logged as debug. If the caller wants to
            // log the exception message, it can.
            final String eMsg = "Unable to contact server @ " + addr + ": " + exc;
            if (debug) {
                log.debug(eMsg);
            }

            throw new AgentCallbackClientException(eMsg);
        } catch (IOException exc) {
            String msg = exc.getMessage();

            if (debug) {
                log.debug(msg);
            }

            throw new AgentCallbackClientException(msg);
        } catch (LatherRemoteException exc) {
            String eMsg = "Remote error while invoking '" + methodName + ": " + exc;

            if (debug) {
                log.debug(eMsg);
            }

            throw new AgentCallbackClientException(eMsg, exc);
        } catch (IllegalStateException e) {
            if (debug) {
                log.debug("Could not create the LatherHTTPClient instance", e);
            }

            throw new AgentCallbackClientException(e);
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
