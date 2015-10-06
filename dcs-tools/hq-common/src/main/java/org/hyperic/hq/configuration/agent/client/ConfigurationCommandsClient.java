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

package org.hyperic.hq.configuration.agent.client;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.configuration.agent.commands.Configuration_args;

/**
 * Client responsible for configuration commands, used for configuration AND scheduling purposes.
 */
public interface ConfigurationCommandsClient {
    /**
     * Process configuration arguments and trigger pushRuntimeDiscoveryConfig, ScheduleMeasurement,
     * UnscheduleMeasurement accordingly.
     * 
     * @param configurationArgs
     * @throws AgentRemoteException
     */
    void configure(Configuration_args configurationArgs)
        throws AgentRemoteException;
}
