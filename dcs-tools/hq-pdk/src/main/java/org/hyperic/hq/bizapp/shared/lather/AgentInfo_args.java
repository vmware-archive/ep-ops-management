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

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

public abstract class AgentInfo_args
            extends LatherValue
{
    private static final String PROP_USER = "user";
    private static final String PROP_PWORD = "pword";
    private static final String PROP_AGENTIP = "agentIP";

    public AgentInfo_args() {
        super();
    }

    public void setUser(String user) {
        this.setStringValue(PROP_USER, user);
    }

    public String getUser() {
        return this.getStringValue(PROP_USER);
    }

    public void setPword(String pword) {
        this.setStringValue(PROP_PWORD, pword);
    }

    public String getPword() {
        return this.getStringValue(PROP_PWORD);
    }

    public void setAgentIP(String agentIP) {
        this.setStringValue(PROP_AGENTIP, agentIP);
    }

    public String getAgentIP() {
        return this.getStringValue(PROP_AGENTIP);
    }

    @Override
    public void validate()
        throws LatherRemoteException {
        try {
            this.getUser();
            this.getPword();
            this.getAgentIP();
        } catch (LatherKeyNotFoundException exc) {
            throw new LatherRemoteException("All values not set");
        }
        /* Not validating user/pass strings for size /pattern
        Validations, if any, will be performed by the vcops-auth authentication mechanism */
    }
}
