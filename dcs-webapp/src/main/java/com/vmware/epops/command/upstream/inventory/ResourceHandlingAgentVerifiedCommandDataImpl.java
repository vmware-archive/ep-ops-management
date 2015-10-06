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

package com.vmware.epops.command.upstream.inventory;

import java.util.List;

import com.vmware.epops.command.ResourceHandlingAgentVerifiedCommandData;
import com.vmware.epops.command.upstream.AgentVerifiedCommandDataImpl;
import com.vmware.epops.model.RawResource;

public abstract class ResourceHandlingAgentVerifiedCommandDataImpl extends
            AgentVerifiedCommandDataImpl implements ResourceHandlingAgentVerifiedCommandData {

    private final List<RawResource> resources;

    public ResourceHandlingAgentVerifiedCommandDataImpl(String agentToken,
                                                        List<RawResource> resources) {
        super(agentToken);
        this.resources = resources;
    }

    @Override
    public List<RawResource> getRawResources() {
        return resources;
    }

}
