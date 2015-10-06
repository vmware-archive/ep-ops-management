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

package com.vmware.epops.webapp.translators.lather;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.upstream.EmptyAgentResponse;
import com.vmware.epops.command.upstream.inventory.PluginReportCommandData;

/**
 * Handles plugins report from agent
 */
public class PluginSendReportTranslator implements AgentVerifiedLatherCommandTranslator {

    private final static Logger log = LoggerFactory.getLogger(PluginSendReportTranslator.class);
    private final static String COMMAND_NAME = CommandInfo.CMD_PLUGIN_SEND_REPORT;
    private final static String RESYNC_AGENT_PLUGINS = "resyncAgentPlugins";

    @SuppressWarnings("unchecked")
    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {

        if (!(latherValue instanceof PluginReport_args)) {
            log.error("Got {} command request with the wrong values from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }
        log.info("Got {} command request from agent: {}", COMMAND_NAME, agentToken);
        boolean resyncAgentPlugins =
                    Boolean.parseBoolean((String) latherValue.getStringVals().get(RESYNC_AGENT_PLUGINS));
        final Map<String, List<String>> pluginSendReportStringLists = latherValue.getStringLists();
        Map<String, String> pluginFileNameToChecksumMap = getPluginsFromReport(pluginSendReportStringLists, agentToken);
        if (null == pluginFileNameToChecksumMap) {
            log.error("Got {} command request with the wrong values from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }
        log.debug("agent={}, plugins={}", agentToken, pluginFileNameToChecksumMap);
        PluginReportCommandData pluginCommand = new PluginReportCommandData();
        pluginCommand.setAgentToken(agentToken);
        pluginCommand.setPluginFileNameToChecksumMap(pluginFileNameToChecksumMap);
        pluginCommand.setResyncAgentPlugins(resyncAgentPlugins);
        return pluginCommand;
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        // Returning NullLatherValue since the agent currently ignores the response
        return new NullLatherValue();
    }

    private Map<String, String> getPluginsFromReport(Map<String, List<String>> pluginSendReportStringLists,
                                                     String agentToken) {

        final List<String> pluginFileNames = pluginSendReportStringLists.get(PluginReport_args.FILE_NAME);
        final List<String> md5s = pluginSendReportStringLists.get(PluginReport_args.MD5);

        if (pluginFileNames.isEmpty()) {
            log.error("Got {} command request with no plugins. from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }
        if (pluginFileNames.size() != md5s.size()) {
            log.error("Got {} command request with unmatching lists. from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }

        Map<String, String> pluginFileNameToChecksumMap = new HashMap<String, String>(pluginFileNames.size());
        for (int i = 0; i < pluginFileNames.size(); i++) {
            pluginFileNameToChecksumMap.put(pluginFileNames.get(i), md5s.get(i));
        }
        return pluginFileNameToChecksumMap;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return EmptyAgentResponse.class;
    }
}
