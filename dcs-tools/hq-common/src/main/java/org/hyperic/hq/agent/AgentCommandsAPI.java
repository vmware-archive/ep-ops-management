/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.agent;

//WISH: convert to an enum
/**
 * An object which represents the internal Agent commands that can be called from a remote client.
 */

public final class AgentCommandsAPI extends AgentAPIInfo {
    private static final byte MAJOR_VER = 0x00;
    private static final byte MINOR_VER = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    public static final String commandPrefix = "agent:";
    public static final int DEFAULT_PORT = 2144;
    public static final String command_getCurrentAgentBundle =
                commandPrefix + "getCurrentAgentBundle";
    public static final String command_ping = commandPrefix + "ping";
    public static final String command_upgrade = commandPrefix + "upgrade";
    public static final String command_restart = commandPrefix + "restart";
    public static final String command_die = commandPrefix + "die";
    public static final String command_update_files = commandPrefix + "updateFiles";

    public static final String[] commandSet = {
                AgentCommandsAPI.command_ping,
                AgentCommandsAPI.command_getCurrentAgentBundle,
                AgentCommandsAPI.command_upgrade,
                AgentCommandsAPI.command_restart,
                AgentCommandsAPI.command_die,
                AgentCommandsAPI.command_update_files
    };

    public AgentCommandsAPI() {
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }
}
