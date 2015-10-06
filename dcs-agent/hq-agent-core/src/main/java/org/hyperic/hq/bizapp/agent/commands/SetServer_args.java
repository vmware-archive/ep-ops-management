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

package org.hyperic.hq.bizapp.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.bizapp.agent.ProviderInfo;

public class SetServer_args
            extends AgentRemoteValue
{
    private static final String PROP_CAMPROVIDER = "provider";
    private static final String PROP_CAMAGENTTOKEN = "agentToken";
    private static final String PROP_UNIDIRECTIONAL_PORT = "unidirectionalPort";

    public SetServer_args() {
        super();
    }

    public SetServer_args(AgentRemoteValue args)
        throws AgentRemoteException
    {
        this.setProvider(getProvider(args));
    }

    public void setProvider(ProviderInfo provider) {
        this.setValue(PROP_CAMPROVIDER, provider.getProviderAddress());
        this.setValue(PROP_CAMAGENTTOKEN, provider.getAgentToken());
        this.setValue(PROP_UNIDIRECTIONAL_PORT, String.valueOf(provider.getProviderPort()));
    }

    public ProviderInfo getProvider() {
        try {
            return getProvider(this);
        } catch (AgentRemoteException exc) {
            throw new AgentAssertionException("Programming error: " +
                        exc.getMessage());
        }
    }

    private static ProviderInfo getProvider(AgentRemoteValue rVal)
        throws AgentRemoteException {
        int secureCommunicationPort =
                    Integer.valueOf(getReqField(rVal, PROP_UNIDIRECTIONAL_PORT)).intValue();
        return new ProviderInfo(getReqField(rVal, PROP_CAMPROVIDER),
                    getReqField(rVal, PROP_CAMAGENTTOKEN), secureCommunicationPort);
    }

    private static String getReqField(AgentRemoteValue rVal,
                                      String field)
        throws AgentRemoteException
    {
        String res;

        if ((res = rVal.getValue(field)) == null) {
            throw new AgentRemoteException("Remote value does not contain " +
                        "a " + field + " field");
        }
        return res;
    }
}
