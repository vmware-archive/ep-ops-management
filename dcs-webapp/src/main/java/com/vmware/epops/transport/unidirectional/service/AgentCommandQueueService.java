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

package com.vmware.epops.transport.unidirectional.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.hq.transport.unidirectional.AgentVerificationStrategy;

import com.vmware.epops.command.downstream.mail.AgentMailCommandDto;
import com.vmware.epops.command.downstream.mail.AgentMailResponseDto;

public interface AgentCommandQueueService {

    /**
     * @param agentMailCommands
     * @return
     */
    List<InvocationRequest> translateAgentCommands(
                                                   List<AgentMailCommandDto> agentMailCommands);

    /**
     * @param responses
     * @return
     */
    List<AgentMailResponseDto> translateAgentResponses(
                                                       List<InvocationResponse> responses);

    /**
     * @param now
     * @return
     */
    Map<String, Collection<InvocationRequest>> removeAgedOutRequests(long now);

    /**
     * @param verifier
     * @return
     */
    boolean removeOldAgentMessages(AgentVerificationStrategy verifier);

    /**
     * @param correlationId
     * @return
     */
    String getAgentTokenFromCorrelationId(String correlationId);

}
