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

package org.hyperic.hq.configuration.agent;

import org.hyperic.hq.agent.AgentAPIInfo;

/**
 * API info which defines the command set being handled by the configuration handlers
 */
public final class ConfigurationCommandsAPI extends AgentAPIInfo {
    private static final byte MAJOR_VER = 0x00;
    private static final byte MINOR_VER = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    private static final String COMMAN_PREFIX = "cfg:";
    public static final String COMMAND_CONFIGURE =
                COMMAN_PREFIX + "configure";
    public static final String[] COMMANDS_SET = { COMMAND_CONFIGURE };

    public ConfigurationCommandsAPI() {
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }
}
