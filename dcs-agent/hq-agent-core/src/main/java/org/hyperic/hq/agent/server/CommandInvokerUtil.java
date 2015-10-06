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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPI;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.Scanner;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.util.config.ConfigResponse;

public class CommandInvokerUtil {

    private final static Log logger = LogFactory.getLog(CommandInvokerUtil.class);

    // TODO: consider extract AgentAPI().getVersion() and AICommandsAPI().getVersion() into const
    private static int getAICommandsAPIVersion() {
        return new AICommandsAPI().getVersion();
    }

    private static int getAgentAPIVersion() {
        return new AgentAPI().getVersion();
    }

    private static AgentCommand getAgentCommand(String commandName,
                                                AgentRemoteValue commandArg) {
        return new AgentCommand(getAgentAPIVersion(), getAICommandsAPIVersion(),
                    commandName, commandArg);
    }

    private static void processRequestCommand(String commandName,
                                              AgentRemoteValue commandArg)
        throws AgentRemoteException {
        AgentCommand agentCommand = getAgentCommand(commandName, commandArg);
        AgentDaemon.getMainInstance().getCommandDispatcher().processRequest(agentCommand, null, null);
    }

    public static void triggerCommand(String commandName,
                                      AgentRemoteValue commandArg)
        throws AgentRemoteException {
        try {
            logger.info("Triggering " + commandName + " command");
            processRequestCommand(commandName, commandArg);
        } catch (AgentRemoteException e) {
            logger.error("Failed to trigger " + commandName + " command", e);
            throw e;
        }
    }

    public static void triggerStartScanCommand()
        throws AgentRemoteException, AutoinventoryException {
        try {
            logger.info("Triggering start scan command");
            AgentRemoteValue commandArg = new AgentRemoteValue();
            AutoinventoryPluginManager autoinventoryPluginManager =
                        AgentDaemon.getMainInstance().getAutoinventoryPluginManager();
            PlatformResource defaultPlatformResource = Scanner.detectPlatform(autoinventoryPluginManager, null);
            ConfigResponse defaultPlatformConfig = Scanner.getPlatformConfig(defaultPlatformResource);
            ScanConfigurationCore scanConfigurationCore = new ScanConfigurationCore();
            scanConfigurationCore.setConfigResponse(defaultPlatformConfig);
            scanConfigurationCore.toAgentRemoteValue(AICommandsAPI.PROP_SCANCONFIG, commandArg);
            processRequestCommand(AICommandsAPI.command_startScan, commandArg);
        } catch (AgentRemoteException e) {
            logger.error("Failed to trigger start scan command", e);
            throw e;
        } catch (AutoinventoryException e) {
            logger.error("Failed to trigger start scan command", e);
            throw e;
        }
    }

    public static void triggerStopScanCommand()
        throws AgentRemoteException {
        try {
            logger.info("Triggering stop scan command");
            processRequestCommand(AICommandsAPI.command_stopScan, null);
        } catch (AgentRemoteException e) {
            // comment it as usually is it just : No scan is currently running.
            // logger.error("Failed to trigger stop scan command", e);
            throw e;
        }
    }

}
