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

package com.vmware.epops.command.downstream.mail.control;

import com.vmware.epops.command.downstream.mail.AbstractAgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;

public class RemovePluginCommand extends AbstractAgentMailCommand {

    public static final AgentMailCommandType COMMAND_TYPE = AgentMailCommandType.REMOVE_PLUGIN;

    private String pluginName;

    public RemovePluginCommand(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public AgentMailCommandType getCommandType() {
        return COMMAND_TYPE;
    }

}
