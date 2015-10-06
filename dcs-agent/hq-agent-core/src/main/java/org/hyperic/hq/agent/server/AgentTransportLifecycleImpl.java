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

package org.hyperic.hq.agent.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.transport.AgentTransport;
import org.hyperic.hq.util.properties.PropertiesUtil;

/**
 * The class that manages the agent transport lifecycle.
 */
public final class AgentTransportLifecycleImpl implements AgentTransportLifecycle {

    private static final Log _log = LogFactory.getLog(AgentTransportLifecycleImpl.class);

    private static final String REMOTE_TRANSPORT_LOCATOR_PATH = "ServerInvokerServlet";

    private final AgentDaemon _agent;
    private final AgentConfig _config;
    private final AgentStorageProvider _storageProvider;
    private final Map _serviceInterfaceName2ServiceInterface;
    private final Map _serviceInterface2ServiceImpl;
    private final Set<ShouldShutdownGracefully> _serviceInterface2WaitAtGracefullStop;

    private AgentTransport _agentTransport;

    public AgentTransportLifecycleImpl(AgentDaemon agent,
                                       AgentConfig bootConfig,
                                       AgentStorageProvider storageProvider) {
        _agent = agent;
        _config = bootConfig;
        _storageProvider = storageProvider;
        _serviceInterfaceName2ServiceInterface = new HashMap();
        _serviceInterface2ServiceImpl = new HashMap();
        _serviceInterface2WaitAtGracefullStop = new HashSet<ShouldShutdownGracefully>();

        // Normally we don't want 'this' to escape the constructor, but
        // we made this class final so we don't have to worry about a
        // not fully initialized instance of a subclass (of this class)
        // handled by another thread.

        // register handler to be notified when the transport layer
        // configuration is finally set
        _agent.registerNotifyHandler(this, CommandsAPIInfo.NOTIFY_SERVER_SET);
    }

    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#startAgentTransport()
     */
    public void startAgentTransport()
        throws Exception {
        // Start the agent transport here - only if all configuration properties
        // are available thru boot props - before starting, register all the services with the agent transport

        // Boot properties override stored values
        ProviderInfo provider = CommandsAPIInfo.getProvider(_storageProvider);

        Properties bootProperties = _config.getBootProperties();

        _log.info("Agent is using unidirectional transport. " +
                    "Looking for properties to start the unidirectional transport.");

        String host = getHost(bootProperties, provider);

        if (host == null) {
            _log.info("Host is not currently set.");
        } else {
            _log.info("Host=" + host);
        }

        int unidirectionalPort = getUndirectionalPort(bootProperties, provider);

        if (unidirectionalPort == -1) {
            _log.info("Unidirectional port is not currently set.");
            _log.info("Cannot start new transport since we do not " +
                        "know the server port for the unidirectional transport.");
            return;
        } else {
            _log.info("Unidirectional port=" + unidirectionalPort);
        }

        long pollingFrequency = getPollingFrequency(bootProperties);

        _log.info("Polling frequency=" + pollingFrequency);

        if (host == null) {
            _log.info("Cannot start new transport since we do not know the host.");

            return;
        }

        _log.info("Setting up unidirectional transport");

        if (_config.isProxyServerSet()) {
            _log.info("Configuring proxy host and port: host=" +
                        _config.getProxyIp() + "; port=" + _config.getProxyPort());

            System.setProperty("https.proxyHost", _config.getProxyIp());
            System.setProperty("https.proxyPort", String.valueOf(_config.getProxyPort()));
        }

        _agentTransport =
                    new AgentTransport(_config, _storageProvider, pollingFrequency, 1);

        if (_agentTransport != null) {

            // register the services and start the server
            for (Iterator iter = _serviceInterface2ServiceImpl.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                Class serviceInterface = (Class) entry.getKey();
                Object serviceImpl = entry.getValue();
                _agentTransport.registerService(serviceInterface, serviceImpl);
            }

            _agentTransport.start();
        }

    }

    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#stopAgentTransport()
     */
    public void stopAgentTransport() {
        if (_agentTransport != null) {
            try {
                _agentTransport.stop();
            } catch (InterruptedException e) {
            }
            _agentTransport = null;
        }
    }

    public void stopAgentTransportGracefully() {
        _log.info("Stopping AgentTransport gracefully");
        stopAgentTransport();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (final ShouldShutdownGracefully serviceImpl : _serviceInterface2WaitAtGracefullStop) {
            executorService.submit(new Runnable() {
                public void run() {
                    serviceImpl.shutdownGracefully();
                }
            });
        }
        try {
            // Wait for all the invocation requests to finish
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MINUTES); // limit the waiting.
        } catch (InterruptedException e) {
            _log.error("Failed to wait for graceful stop", e);
        }
        _log.info("Stopping AgentTransport gracefully: done");
    }

    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#handleNotification(java.lang.String, java.lang.String)
     */
    public void handleNotification(String msgClass,
                                   String msg) {
        ProviderInfo provider = CommandsAPIInfo.getProvider(_storageProvider);

        // Start the agent transport if configuration properties were
        // not available on the original start attempt.
        if (_agentTransport == null) {
            try {
                startAgentTransport();
            } catch (Exception e) {
                _log.error("Failed to start agent transport after agent setup", e);
                return;
            }
        }

        // If the agent transport is still not started, then we have a problem!
        if (_agentTransport == null) {
            _log.error("Failed to start agent transport after agent setup");

            return;
        }

        // Update the agent transport agent token
        if (provider == null) {
            _log.error("Agent transport expected agent token set but " +
                        "storage provider does not have token.");
        } else {
            if (_agentTransport != null) {
                String agentToken = provider.getAgentToken();

                _log.info("Updating agent transport with new agent token: " + agentToken);
            }
        }
    }

    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#registerService(java.lang.Class, java.lang.Object)
     */
    public void registerService(Class serviceInterface,
                                Object serviceImpl) {

        Class oldInterface = (Class) _serviceInterfaceName2ServiceInterface.get(serviceInterface.getName());

        if (oldInterface == null) {
            _serviceInterfaceName2ServiceInterface.put(serviceInterface.getName(), serviceInterface);
            _serviceInterface2ServiceImpl.put(serviceInterface, serviceImpl);
        } else {
            _serviceInterfaceName2ServiceInterface.remove(serviceInterface.getName());
            _serviceInterface2ServiceImpl.remove(oldInterface);
            _serviceInterfaceName2ServiceInterface.put(serviceInterface.getName(), serviceInterface);
            _serviceInterface2ServiceImpl.put(serviceInterface, serviceImpl);
        }

        if (null != serviceImpl && serviceImpl instanceof ShouldShutdownGracefully) {
            _serviceInterface2WaitAtGracefullStop.add((ShouldShutdownGracefully) serviceImpl);
        }
    }

    private String getHost(Properties bootProperties,
                           ProviderInfo provider) {
        String host = bootProperties.getProperty(AgentConfig.QPROP_IPADDR);

        if (host == null && provider != null) {
            host = AgentCallbackClient.getHostFromProviderURL(provider.getProviderAddress());
        }

        return host;
    }

    private int getUndirectionalPort(Properties bootProperties,
                                     ProviderInfo provider) {
        int port = -1;

        String portString =
                    bootProperties.getProperty(AgentConfig.QPROP_SSLPORT);

        if (portString == null) {
            if (provider != null) {
                port = provider.getProviderPort();
            }
        } else {
            try {
                port = PropertiesUtil.getPortValue(AgentConfig.QPROP_SSLPORT, portString, HQConstants.DEFAULT_SSL_PORT);
            } catch (NumberFormatException e) {
                _log.error("The port given in the " + AgentConfig.QPROP_SSLPORT + " property of " + portString
                            + " is not a valid number.");
            } catch (IllegalArgumentException e) {
                _log.error("The port given in the " + AgentConfig.QPROP_SSLPORT + " property is empty.");
            }
        }

        return port;
    }

    private long getPollingFrequency(Properties bootProperties) {
        String pollingFrequencyString = bootProperties.getProperty(AgentConfig.QPROP_UNI_POLLING_FREQUENCY);
        return PropertiesUtil.getLongValue(AgentConfig.QPROP_UNI_POLLING_FREQUENCY, pollingFrequencyString,
                    AgentConfig.DEFAULT_POLLING_FREQUENCY_IN_MS);
    }

}
