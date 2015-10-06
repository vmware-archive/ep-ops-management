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

package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class UnscheduleMeasurementsById_args extends AgentRemoteValue {

    private final String RESOURCE_ID = "RESOURCE_ID";

    public UnscheduleMeasurementsById_args(int resourceInternalId) {
        super();
        setResourceInternalId(resourceInternalId);
    }

    public UnscheduleMeasurementsById_args(AgentRemoteValue args)
        throws AgentRemoteException {
        super();
        int resourceId = args.getValueAsInt(RESOURCE_ID);
        setResourceInternalId(resourceId);
    }

    public int getResourceInternalId()
        throws AgentRemoteException {
        return getValueAsInt(RESOURCE_ID);
    }

    private void setResourceInternalId(int resourceInternalId) {
        setValue(RESOURCE_ID, Integer.toString(resourceInternalId));
    }
}
