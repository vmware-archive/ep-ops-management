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

package com.vmware.epops.webapp.translators.agent.impl;

import javax.annotation.PostConstruct;

import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClient;
import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;
import com.vmware.epops.command.downstream.mail.IllegalCommandException;
import com.vmware.epops.command.downstream.mail.IllegalCommandException.CommandError;
import com.vmware.epops.command.downstream.mail.autoinventory.StartScanCommand;
import com.vmware.epops.transport.unidirectional.proxies.AgentProxy;
import com.vmware.epops.transport.unidirectional.utils.AgentTranslatorsMap;
import com.vmware.epops.webapp.translators.agent.AgentCommandTranslator;

/*
 * translator from AgentAutoInventoryCommand to InvocationRequest
 */
@Component
public class AgentAutoInventoryCommandsTranslator implements AgentCommandTranslator {

    private final static Logger log = LoggerFactory.getLogger(AgentAutoInventoryCommandsTranslator.class);

    @Autowired
    private AgentTranslatorsMap agentTranslatorsMap;
    private AgentProxy agentAutoInventoryProxy;

    @PostConstruct
    public void init() {
        // register commands you want to translate via this translator
        agentAutoInventoryProxy = new AgentProxy(AICommandsClient.class);
        agentTranslatorsMap.registerTranslator(StartScanCommand.COMMAND_TYPE, this);
    }

    @Override
    public InvocationRequest translateCommand(AgentMailCommand agentMailCommand)
        throws Throwable {
        if (!(agentMailCommand instanceof StartScanCommand)) {
            throw new IllegalCommandException(CommandError.COMMAND_NOT_SUPPORTED, agentMailCommand.getCommandDetails(),
                        this.getClass().getSimpleName());
        }
        return translateCommand((StartScanCommand) agentMailCommand);
    }

    private InvocationRequest translateCommand(StartScanCommand agentMailCommand)
        throws Throwable { // NOPMD
        log.debug("translating command:" + agentMailCommand.getCommandType());
        Class<?>[] parameterTypes = new Class<?>[] { ScanConfigurationCore.class };
        Object[] args = new Object[] { new ScanConfigurationCore() };
        return agentAutoInventoryProxy.getInvocationRequest("startScan", parameterTypes, args);
    }

    @Override
    public AgentMailResponse translateResponse(AgentMailCommandType commandType,
                                               InvocationResponse response) {
        switch (commandType) {
            case START_AUTO_DISCOVERY:
                return null;
            default:
                throw new IllegalArgumentException("response for " + commandType
                            + " is not implemented");
        }
    }

}
