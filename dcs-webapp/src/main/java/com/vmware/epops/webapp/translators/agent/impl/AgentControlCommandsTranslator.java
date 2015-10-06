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

import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;
import com.vmware.epops.command.downstream.mail.IllegalCommandException;
import com.vmware.epops.command.downstream.mail.IllegalCommandException.CommandError;
import com.vmware.epops.command.downstream.mail.control.RemovePluginCommand;
import com.vmware.epops.transport.unidirectional.proxies.AgentProxy;
import com.vmware.epops.transport.unidirectional.utils.AgentTranslatorsMap;
import com.vmware.epops.webapp.translators.agent.AgentCommandTranslator;

/*
 * translator from AgentControlCommand to InvocationRequest
 */
@Component
public class AgentControlCommandsTranslator implements AgentCommandTranslator {

    private final static Logger log = LoggerFactory.getLogger(AgentControlCommandsTranslator.class);

    @Autowired
    private AgentTranslatorsMap agentTranslatorsMap;
    AgentProxy agentControlProxy;

    @PostConstruct
    public void init() {
        agentControlProxy = new AgentProxy(ControlCommandsClient.class);
        agentTranslatorsMap.registerTranslator(RemovePluginCommand.COMMAND_TYPE, this);
    }

    @Override
    public InvocationRequest translateCommand(AgentMailCommand agentMailCommand)
        throws Throwable {
        if (!(agentMailCommand instanceof RemovePluginCommand)) {
            throw new IllegalCommandException(CommandError.COMMAND_NOT_SUPPORTED, agentMailCommand.getCommandDetails(),
                        this.getClass().getSimpleName());
        }
        return translateCommand((RemovePluginCommand) agentMailCommand);
    }

    private InvocationRequest translateCommand(RemovePluginCommand agentMailCommand)
        throws Throwable { // NOPMD
        log.debug("translating command:" + agentMailCommand.getCommandType());
        RemovePluginCommand removePluginCommand = agentMailCommand;
        String methodName = "controlPluginRemove";
        Class<?>[] parameterTypes = new Class<?>[] { String.class };
        Object[] args = new Object[] { removePluginCommand.getPluginName() };
        return agentControlProxy.getInvocationRequest(methodName, parameterTypes, args);
    }

    @Override
    public AgentMailResponse translateResponse(AgentMailCommandType commandType,
                                               InvocationResponse response) {
        return null;
    }

}
