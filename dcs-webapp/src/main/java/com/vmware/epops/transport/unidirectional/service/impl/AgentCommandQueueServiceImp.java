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

package com.vmware.epops.transport.unidirectional.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.hq.transport.unidirectional.AgentVerificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandDto;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;
import com.vmware.epops.command.downstream.mail.AgentMailResponseDto;
import com.vmware.epops.transport.unidirectional.service.AgentCommandQueueService;
import com.vmware.epops.webapp.translators.agent.AgentMailCommandTranslatorFacade;

/**
 * A service residing on the EP Ops webapp that responds to polling requests from the EP Ops agent (which using the
 * unidrectional=yes).
 */
@Service("agentCommandQueueService")
public class AgentCommandQueueServiceImp implements AgentCommandQueueService {

    /**
     * group name delimiter in metric key
     */
    public static final String PARSE_DELIMITER = "\\|";

    private final static Logger log = LoggerFactory
                .getLogger(AgentCommandQueueServiceImp.class);
    private final static String CORRELATIONID_CHAR = "#";

    @Autowired
    private AgentMailCommandTranslatorFacade translator;

    @Override
    public List<InvocationRequest> translateAgentCommands(
                                                          List<AgentMailCommandDto> agentMailCommands) {
        AgentMailCommand currCommand;
        List<InvocationRequest> rtn = new ArrayList<InvocationRequest>(
                    agentMailCommands.size());
        for (AgentMailCommandDto commandDto : agentMailCommands) {
            InvocationRequest invocationRequest;
            try {
                currCommand = commandDto.getAgentMailCommand();
                invocationRequest = translator.translateCommand(currCommand);
                if (invocationRequest != null) {
                    String correlationId = commandDto.getCommandUUID().toString();
                    invocationRequest.setSessionId(correlationId);
                    rtn.add(invocationRequest);
                }
            } catch (Throwable e) { // NOPMD
                log.error("Error: {} for command: {}", e.toString(), commandDto.getCommandUUID());
            }
        }
        return rtn;
    }

    @Override
    public List<AgentMailResponseDto> translateAgentResponses(
                                                              List<InvocationResponse> responses) {
        AgentMailResponseDto currResponseDto;
        List<AgentMailResponseDto> agentMailResponses = new ArrayList<AgentMailResponseDto>();
        for (InvocationResponse response : responses) {
            if (log.isDebugEnabled()) {
                log.debug("response :" + ", SessionId="
                            + response.getSessionId() + ", Result="
                            + response.getPayload() + ", response  " + response);
            }
            AgentMailCommandType commandType = extractCommandType(response);
            if (commandType == null) {
                log.error("response for unknown(null) Command Type - " + response.getSessionId());
                continue;
            }
            AgentMailResponse translated = translator.translateResponse(
                        commandType, response);
            currResponseDto = new AgentMailResponseDto(response.getSessionId(),
                        translated);
            agentMailResponses.add(currResponseDto);
        }
        return agentMailResponses;
    }

    private AgentMailCommandType extractCommandType(InvocationResponse response) {
        String sessionId = response.getSessionId();
        if (sessionId == null) {
            return null;
        }
        String[] attributes = sessionId.split(PARSE_DELIMITER);
        return AgentMailCommandType.valueOf(attributes[2]);
    }

    @Override
    public String getAgentTokenFromCorrelationId(String correlationId) {
        String agentToken = null;
        try {
            agentToken = correlationId.substring(0,
                        correlationId.indexOf(CORRELATIONID_CHAR));
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return agentToken;
    }

    public String getCommandUUIDFromCorrelationId(String correlationId) {
        String commandUUID = null;
        try {
            commandUUID = correlationId.substring(correlationId
                        .indexOf(CORRELATIONID_CHAR) + 1);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return commandUUID;
    }

    @Override
    public Map<String, Collection<InvocationRequest>> removeAgedOutRequests(
                                                                            long now) {
        return null;
    }

    @Override
    public boolean removeOldAgentMessages(AgentVerificationStrategy verifier) {
        return false;
    }

    public InvocationResponse invoke(String agentToken,
                                     InvocationRequest invocationRequest)
        throws InterruptedException {
        return null;
    }

}
