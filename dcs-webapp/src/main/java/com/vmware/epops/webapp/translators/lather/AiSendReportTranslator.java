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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.ScanMethodState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.upstream.EmptyAgentResponse;
import com.vmware.epops.command.upstream.inventory.AiSendReportCommandData;
import com.vmware.epops.model.RawResource;

/**
 * Handles auto inventory report from agent that contains platforms and servers
 * 
 * @author Liat
 */

public class AiSendReportTranslator extends AiReportTranslator {

    private final static Logger log = LoggerFactory.getLogger(AiSendReportTranslator.class);
    private final static String COMMAND_NAME = CommandInfo.CMD_AI_SEND_REPORT;

    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {
        if (StringUtils.isEmpty(agentToken)) {
            log.error("Got {} command request with an empty agentToken", COMMAND_NAME);
            return null;
        }
        if (!(latherValue instanceof AiSendReport_args)) {
            log.error("Got {} command request with the wrong values from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }

        AiSendReport_args latherArgs = (AiSendReport_args) latherValue;
        ScanStateCore core = latherArgs.getCore();

        if (!isInputValid(core)) {
            return null;
        }
        ReportParams reportParams = new ReportParams();
        reportParams.setAgentToken(agentToken);
        reportParams.setSync(isSyncReport(latherValue));

        if (reportParams.isSync()) {
            log.info("Got {}(sync) command request from agent: {}", COMMAND_NAME, agentToken);
        }

        RawResource platformResource = createPlatformResource(reportParams, core);
        AiSendReportCommandData commandData =
                    new AiSendReportCommandData(platformResource);
        return commandData;
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        // Returning NullLatherValue since the agent currently ignores the response
        return new NullLatherValue();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private boolean isInputValid(ScanStateCore core) {
        if (core == null) {
            log.error("Got a null core in {} command request", COMMAND_NAME);
            return false;
        }
        if (core.getPlatform() == null) {
            log.warn("Got null platform in {} command request", COMMAND_NAME);
            return false;
        }
        if (core.getScanMethodStates() == null) {
            log.warn("Got null servers in {} command request", COMMAND_NAME);
            return false;
        }
        return true;
    }

    private RawResource createPlatformResource(ReportParams reportParams,
                                               ScanStateCore core) {
        RawResource platformResource = createPlatformResource(reportParams, core.getPlatform(), false);
        List<AIServerExtValue> servers = getServerResources(core.getScanMethodStates());
        List<RawResource> serverResources = new ArrayList<RawResource>();
        for (AIServerExtValue server : servers) {
            serverResources.add(createServerResource(reportParams, server, false));
        }
        platformResource.setChildren(serverResources);
        return platformResource;
    }

    private List<AIServerExtValue> getServerResources(ScanMethodState[] states) {
        List<AIServerExtValue> extServers = new ArrayList<AIServerExtValue>();
        for (ScanMethodState state : states) {
            AIServerValue[] servers = state.getServers();
            if (servers == null) {
                continue;
            }
            for (AIServerValue server : servers) {
                extServers.add((AIServerExtValue) server);
            }
        }
        return extServers;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return EmptyAgentResponse.class;
    }

}
