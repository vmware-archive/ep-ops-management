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

import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.SendAgentResponses_args;
import org.hyperic.hq.bizapp.shared.lather.SendAgentResponses_result;
import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.lather.LatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.downstream.mail.AgentMailResponseDto;
import com.vmware.epops.command.upstream.queue.ReturnAgentResultCommandData;
import com.vmware.epops.command.upstream.queue.ReturnAgentResultCommandResponse;
import com.vmware.epops.transport.unidirectional.service.AgentCommandQueueService;

public class SendAgentResponseCommandTranslator implements AgentVerifiedLatherCommandTranslator {

    private final static Logger log = LoggerFactory
                .getLogger(SendAgentResponseCommandTranslator.class);
    private final static String COMMAND_NAME = CommandInfo.CMD_SEND_RESPONSE;

    private final AgentCommandQueueService agentCommandQueueService;

    public SendAgentResponseCommandTranslator(AgentCommandQueueService agentCommandQueueServiceImpl) {
        this.agentCommandQueueService = agentCommandQueueServiceImpl;
    }

    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {
        if (!(latherValue instanceof SendAgentResponses_args)) {
            log.error("Got {} command request with the wrong values from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }
        SendAgentResponses_args args = (SendAgentResponses_args) latherValue;
        List<InvocationResponse> responses = args.getResponses();
        if (responses != null) {
            List<AgentMailResponseDto> agentMailResponses = agentCommandQueueService
                        .translateAgentResponses(responses);
            ReturnAgentResultCommandData returnAgentResultCommandData = new ReturnAgentResultCommandData(
                        agentToken, agentMailResponses);
            return returnAgentResultCommandData;
        } else {
            log.warn("No responses receieved from agent: " + agentToken + ".");
            return null;
        }
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        SendAgentResponses_result result = new SendAgentResponses_result();
        if (null != response.getErrorString()
                    && !response.getErrorString().isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage(response.getErrorString());
        } else {
            result.setSuccess(true);
        }
        return result;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return ReturnAgentResultCommandResponse.class;
    }
}
