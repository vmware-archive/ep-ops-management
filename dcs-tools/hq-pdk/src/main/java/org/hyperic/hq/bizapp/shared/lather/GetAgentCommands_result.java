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

package org.hyperic.hq.bizapp.shared.lather;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.lather.LatherValue;

public class GetAgentCommands_result extends LatherValue {

    public static String AGENT_COMMANDS = "agentCommands";

    @SuppressWarnings("unchecked")
    public List<InvocationRequest> getAgentCommands() {
        if (null != getObject(AGENT_COMMANDS)) {
            return (List<InvocationRequest>) getObject(AGENT_COMMANDS);
        }
        return new ArrayList<InvocationRequest>();
    }

    public void setAgentCommands(List<InvocationRequest> agentCommands) {
        addObject(AGENT_COMMANDS, (Serializable) agentCommands);
    }

    @Override
    public String toString() {
        return "GetAgentCommands_result [Commands: '" + getAgentCommands() + "']";
    }
}
