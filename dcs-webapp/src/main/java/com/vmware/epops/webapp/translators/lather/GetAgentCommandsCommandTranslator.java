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

package com.vmware.epops.webapp.translators.lather;

import java.util.List;

import org.hyperic.hq.bizapp.shared.lather.GetAgentCommands_result;
import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.lather.LatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.downstream.mail.AgentMailCommandDto;
import com.vmware.epops.command.upstream.queue.GetAgentQueueCommandData;
import com.vmware.epops.command.upstream.queue.GetAgentQueueCommandResponse;
import com.vmware.epops.transport.unidirectional.service.AgentCommandQueueService;

public class GetAgentCommandsCommandTranslator implements AgentVerifiedLatherCommandTranslator {

    private final static Logger log = LoggerFactory
                .getLogger(GetAgentCommandsCommandTranslator.class);

    private final AgentCommandQueueService agentCommandQueueService;

    public GetAgentCommandsCommandTranslator(AgentCommandQueueService agentCommandQueueServiceImpl) {
        this.agentCommandQueueService = agentCommandQueueServiceImpl;
    }

    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {
        GetAgentQueueCommandData getAgentQueueCommandData = new GetAgentQueueCommandData(
                    agentToken);
        return getAgentQueueCommandData;
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        if (!(response instanceof GetAgentQueueCommandResponse)) {
            log.error("response must be of type GetAgentQueueCommandResponse");
            return null;
        }
        GetAgentQueueCommandResponse getAgentQueueCommandResponse = (GetAgentQueueCommandResponse) response;
        List<AgentMailCommandDto> agentMailCommands = getAgentQueueCommandResponse
                    .getAgentCommandsQueue();
        List<InvocationRequest> requests = agentCommandQueueService
                    .translateAgentCommands(agentMailCommands);
        GetAgentCommands_result result = new GetAgentCommands_result();
        result.setAgentCommands(requests);
        return result;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return GetAgentQueueCommandResponse.class;
    }
}
