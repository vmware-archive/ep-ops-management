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

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.bizapp.agent.ProviderInfo;

public class GetServer_result
            extends AgentRemoteValue
{
    private static final String PROP_SERVER = "server";
    private static final String PROP_AGENTTOKEN = "agentToken";
    private static final String PROP_UNIDIRECTIONAL_PORT = "unidirectionalPort";

    public GetServer_result() {
        super();
    }

    public GetServer_result(AgentRemoteValue args) {
        this.setProvider(getProvider(args));
    }

    public void setProvider(ProviderInfo provider) {
        if (provider != null) {
            this.setValue(PROP_SERVER, provider.getProviderAddress());
            this.setValue(PROP_AGENTTOKEN, provider.getAgentToken());
            this.setValue(PROP_UNIDIRECTIONAL_PORT, String.valueOf(provider.getProviderPort()));
        }
    }

    public ProviderInfo getProvider() {
        return getProvider(this);
    }

    public static ProviderInfo getProvider(AgentRemoteValue args) {
        String server, agentToken;

        if ((server = args.getValue(PROP_SERVER)) == null ||
                    (agentToken = args.getValue(PROP_AGENTTOKEN)) == null)
        {
            return null;
        }

        int secureCommunicationPort =
                    Integer.valueOf(args.getValue(PROP_UNIDIRECTIONAL_PORT)).intValue();

        return new ProviderInfo(server, agentToken, secureCommunicationPort);
    }

}
