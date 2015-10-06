/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class AgentBundle_result extends AgentRemoteValue {

    private static final String PARAM_AGENTBUNDLE = "agentbundle";

    public AgentBundle_result(String currentAgentBundle) {
        super.setValue(AgentBundle_result.PARAM_AGENTBUNDLE, currentAgentBundle);
    }

    public AgentBundle_result(AgentRemoteValue args)
        throws AgentRemoteException {
        this(args.getValue(AgentBundle_result.PARAM_AGENTBUNDLE));
    }

    public String getCurrentAgentBundle() {
        return this.getValue(AgentBundle_result.PARAM_AGENTBUNDLE);
    }

    public void setValue(String key,
                         String val) {
        throw new AgentAssertionException("This should never be called");
    }

}
