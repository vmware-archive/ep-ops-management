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

package org.hyperic.hq.configuration.agent.server;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentTransportLifecycle;
import org.hyperic.hq.configuration.agent.ConfigurationCommandsAPI;
import org.hyperic.hq.configuration.agent.client.ConfigurationCommandsClient;
import org.hyperic.hq.configuration.agent.commands.Configuration_args;
import org.hyperic.hq.configuration.agent.commands.Configuration_result;

/**
 * Configuration Commands Server handles configuration commands arriving, Invoking them on the relevant Service command.
 */
public class ConfigurationCommandsServer implements AgentServerHandler {
    private static final Log log = LogFactory.getLog(ConfigurationCommandsServer.class);
    private final ConfigurationCommandsAPI configurationCommandsAPI;
    private ConfigurationCommandsService configurationCommandsService;

    public ConfigurationCommandsServer() {
        configurationCommandsAPI = new ConfigurationCommandsAPI();
    }

    public String[] getCommandSet() {
        return ConfigurationCommandsAPI.COMMANDS_SET;
    }

    public AgentAPIInfo getAPIInfo() {
        return configurationCommandsAPI;
    }

    public AgentRemoteValue dispatchCommand(String cmd,
                                            AgentRemoteValue args,
                                            InputStream inStream,
                                            OutputStream outStream)
        throws AgentRemoteException {
        if (ConfigurationCommandsAPI.COMMAND_CONFIGURE.equals(cmd)) {
            /**
             * We can only initialize the service after this class startup method is called. Until that we need to
             * reject all calls to this method. This should never happen, because the order of things is first to start
             * up all handlers and then call their dispatchCommand method.
             */
            if (configurationCommandsService == null) {
                throw new AgentRemoteException("Dispatch command was invoked before configuration "
                            + "service initialized.");
            }
            Configuration_args configurationArgs = (Configuration_args) args;
            configurationCommandsService.configure(configurationArgs);
            return new Configuration_result();
        } else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    public void startup(AgentDaemon agent)
        throws AgentStartException {
        AgentTransportLifecycle agentTransportLifecycle = getTransportLifecycle(agent);
        // We need to pass command dispatcher to the service when we this class
        // dispatchCommand is called.
        configurationCommandsService = new ConfigurationCommandsService(agent);
        registerCommandsService(agentTransportLifecycle);

        log.info("Configuration Commands Server started up");
    }

    private void registerCommandsService(AgentTransportLifecycle agentTransportLifecycle)
        throws AgentStartException {
        log.info("Registering Configuration Commands Service with Agent Transport");
        try {
            agentTransportLifecycle.registerService(ConfigurationCommandsClient.class,
                        configurationCommandsService);
        } catch (Exception e) {
            throw new AgentStartException("Failed to register Configuration Commands Service", e);
        }
    }

    private AgentTransportLifecycle getTransportLifecycle(AgentDaemon agent)
        throws AgentStartException {
        AgentTransportLifecycle agentTransportLifecycle;
        try {
            agentTransportLifecycle = agent.getAgentTransportLifecycle();
        } catch (Exception e) {
            throw new AgentStartException("Unable to get agent transport lifecycle: " +
                        e.getMessage());
        }

        return agentTransportLifecycle;
    }

    public void postInitActions()
        throws AgentStartException {
    }

    public void shutdown() {
        log.info("Resource Configuration Commands Server shut down");
    }

    public void refreshOnPluginsChange() {
        log.info("Agent commands refreshOnPluginsChange");
    }
}
