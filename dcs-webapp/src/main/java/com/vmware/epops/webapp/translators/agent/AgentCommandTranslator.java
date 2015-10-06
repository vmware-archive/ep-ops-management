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

import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;

/*
 * interface for translators from AgentMailCommand to InvocationRequest
 */
public interface AgentCommandTranslator {

    /**
     * Translate a AgentMailCommand to invocation request
     * 
     * @param agentMailCommand
     * @return InvocationRequest
     * @throws Throwable
     */
    public InvocationRequest translateCommand(AgentMailCommand agentMailCommand)
        throws Throwable;

    /**
     * Translate an InvocationResponse into a AgentMailResponse object
     * 
     * @param response
     * @return AgentMailResponse
     */
    public AgentMailResponse translateResponse(AgentMailCommandType commandType,
                                               InvocationResponse response);

}
