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

package org.hyperic.hq.configuration.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class Configuration_result extends AgentRemoteValue {
    public Configuration_result() {
        super();
    }

    public Configuration_result(AgentRemoteValue args)
        throws AgentRemoteException {
    }

    public void setValue(String key,
                         String val) {
        throw new AgentAssertionException("This should never be called");
    }
}
