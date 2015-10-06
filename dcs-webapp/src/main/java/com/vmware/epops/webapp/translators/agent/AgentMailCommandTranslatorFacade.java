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

package com.vmware.epops.webapp.translators.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;
import com.vmware.epops.command.downstream.mail.IllegalCommandException;
import com.vmware.epops.command.downstream.mail.IllegalCommandException.CommandError;
import com.vmware.epops.transport.unidirectional.utils.AgentTranslatorsMap;

/*
 * service for translating from AgentMailCommand to InvocationRequest
 * calls the specific translator based on the command type
 */
@Service
public class AgentMailCommandTranslatorFacade implements AgentCommandTranslator {

    private final static Logger log = LoggerFactory.getLogger(AgentMailCommandTranslatorFacade.class);

    @Autowired
    AgentTranslatorsMap agentTranslatorsMap;

    @Override
    public InvocationRequest translateCommand(AgentMailCommand agentMailCommand)
        throws Throwable { // NOPMD

        AgentMailCommandType commandType = agentMailCommand.getCommandType();
        log.debug("translating command:" + commandType);
        AgentCommandTranslator translator = agentTranslatorsMap.getTranslator(commandType);
        if (translator == null) {
            throw new IllegalCommandException(CommandError.COMMAND_NOT_IMPLEMENTED,
                        agentMailCommand.getCommandDetails(), this.getClass().getSimpleName());
        }
        return translator.translateCommand(agentMailCommand);
    }

    @Override
    public AgentMailResponse translateResponse(AgentMailCommandType commandType,
                                               InvocationResponse response) {
        AgentCommandTranslator translator = agentTranslatorsMap.getTranslator(commandType);
        return translator.translateResponse(commandType, response);
    }

}
