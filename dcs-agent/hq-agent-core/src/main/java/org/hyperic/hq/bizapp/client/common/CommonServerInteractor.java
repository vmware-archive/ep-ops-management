package org.hyperic.hq.bizapp.client.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HttpContext;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.lather.client.LatherHTTPClient;
import org.hyperic.util.http.ServerHttpClient;

/**
 * Common Server Interactor is a service, providing reusable http clients, for efficient agent to server communication.
 * The rationale of reusing a client is to avoid client initialization overhead of each request.
 */
public enum CommonServerInteractor {
    INSTANCE;

    private static final int FALLBACK_KEEP_ALIVE_DURATION = 15 * 1000 /*
                                                                      * 15
                                                                      * seconds
                                                                      */;
    private static final int CONNECTION_TIMEOUT = 30 * 1000 /* 30 seconds */;
    private static final int SOCKET_TIMEOUT = 5 * 60 * 1000 /* 5 minutes */;
    private static final Log logger = LogFactory
                .getLog(CommonServerInteractor.class);
    private ServerHttpClient httpClient;

    /**
     * @return reusable optimized client. Meaning that the client is set to keep-alive, and has a connection manager who
     *         manages a connections pool.
     */
    public synchronized ServerHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = generateHttpClient(getFallbackConfig());
        }

        return httpClient;
    }

    private ServerHttpClient generateHttpClient(
                                                AgentKeystoreConfig agentKeystoreConfig) {
        ServerHttpClient client = new ServerHttpClient(agentKeystoreConfig,
                    LatherHTTPClient.getHttpConfig(CONNECTION_TIMEOUT,
                                SOCKET_TIMEOUT), false, generateConnectionManager(),
                    getMaxRequestsPerConnection()
                    , isSupportRRDNS(),
                    getFailPeriodInMin(),
                    getDownPeriodInMin());

        client.setReuseStrategy(new DefaultConnectionReuseStrategy());
        client.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response,
                                             HttpContext context) {
                long serverKeepaliveTimeout = super.getKeepAliveDuration(
                            response, context);

                if (serverKeepaliveTimeout <= 0) {
                    return FALLBACK_KEEP_ALIVE_DURATION;
                }

                return serverKeepaliveTimeout;
            }
        });

        return client;
    }

    private ClientConnectionManager generateConnectionManager() {
        ThreadSafeClientConnManager conman = new ThreadSafeClientConnManager();
        conman.setDefaultMaxPerRoute(getMaxConnectionsPerRoute());

        return conman;
    }

    private AgentKeystoreConfig getFallbackConfig() {
        AgentConfig config = AgentCallbackClient.getAgentConfig();

        return new AgentKeystoreConfig(config);
    }

    private static int getMaxRequestsPerConnection() {
        String maxRequestsPerConnection = AgentConfig.getDefaultProperties()
                    .getProperty(
                                AgentConfig.PROP_MAX_HTTP_REQUESTS_PER_CONNECTION[0]);
        try {
            return Integer.parseInt(maxRequestsPerConnection);
        } catch (NumberFormatException e) {
            return AgentConfig.MAX_HTTP_REQUESTS_PER_CONNECTION;
        }
    }

    private static int getMaxConnectionsPerRoute() {
        String maxConnectionsPerRoute = AgentConfig.getDefaultProperties()
                    .getProperty(AgentConfig.PROP_MAX_HTTP_CONNECTION_PER_ROUTE[0]);
        try {
            return Integer.parseInt(maxConnectionsPerRoute);
        } catch (NumberFormatException e) {
            return AgentConfig.MAX_HTTP_CONNECTION_PER_ROUTE;
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

    /**
     * Whenever the agent's keystore configuration changes, this method needs to be called in order to re-allocate a
     * fresh reusable client.
     * 
     * examples of such cases: (1) when saving user's certificate to the keystore, while configuring the agent. (2) when
     * trusting an unverified certificate.
     * 
     * Note that this method runs in the context of the process. Thus running this method from agent setup will
     * influence only on the process that runs AgentClient and will NOT propagate the changes to AgentDaemon who
     * actually uses these connections. @see CommandsServer.handleKeystoreChangeNotification()
     * 
     * @param agentKeystoreConfig
     */
    public synchronized void onKeystoreChange(
                                              AgentKeystoreConfig agentKeystoreConfig) {
        httpClient = generateHttpClient(agentKeystoreConfig);
        logger.info("Generated a new http client");
    }
}
