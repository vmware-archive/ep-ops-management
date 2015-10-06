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

import java.util.Map;

import com.vmware.epops.command.upstream.AgentVerifiedCommandDataImpl;

public class PluginReportCommandData extends AgentVerifiedCommandDataImpl {

    private String commandName = null;
    private Map<String, String> pluginFileNameToChecksumMap;
    private boolean resyncAgentPlugins;

    public PluginReportCommandData() {
        commandName = "pluginSendReport";
    }

    public Map<String, String> getPluginFileNameToChecksumMap() {
        return pluginFileNameToChecksumMap;
    }

    public void setPluginFileNameToChecksumMap(Map<String, String> pluginFileNameToChecksumMap) {
        this.pluginFileNameToChecksumMap = pluginFileNameToChecksumMap;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    public void setResyncAgentPlugins(boolean resyncAgentPlugins) {
        this.resyncAgentPlugins = resyncAgentPlugins;
    }

    public boolean getResyncAgentPlugins() {
        return this.resyncAgentPlugins;
    }
}
