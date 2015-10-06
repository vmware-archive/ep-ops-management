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
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.bizapp.shared.lather.AiSendRuntimeReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.upstream.EmptyAgentResponse;
import com.vmware.epops.command.upstream.inventory.AiSendRuntimeReportCommandData;
import com.vmware.epops.model.RawResource;

/**
 * Handles runtime report from agent that contains platforms,servers and services
 * 
 * @author Liat
 */
public class AiSendRuntimeReportTranslator extends AiReportTranslator {

    private final static Logger log = LoggerFactory.getLogger(AiSendRuntimeReportTranslator.class);
    private final static String COMMAND_NAME = CommandInfo.CMD_AI_SEND_RUNTIME_REPORT;

    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {

        if (StringUtils.isEmpty(agentToken)) {
            log.error("Got {} command request with an empty agentToken", COMMAND_NAME);
            return null;
        }
        if (!(latherValue instanceof AiSendRuntimeReport_args)) {
            log.error("Got {} command request with the wrong values from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }

        AiSendRuntimeReport_args latherArgs = (AiSendRuntimeReport_args) latherValue;

        ReportParams reportParams = new ReportParams();
        reportParams.setAgentToken(agentToken);
        reportParams.setSync(isSyncReport(latherValue));

        if (reportParams.isSync()) {
            log.info("Got {}(sync) command request from agent: {}", COMMAND_NAME, agentToken);
        }

        List<RawResource> rawResources = new ArrayList<>();
        if ((latherArgs.getReport() != null) && (latherArgs.getReport().getServerReports() != null)) {
            for (RuntimeResourceReport runtimeReport : latherArgs.getReport().getServerReports()) {
                if (runtimeReport.getAIPlatforms() != null) {
                    for (AIPlatformValue platform : runtimeReport.getAIPlatforms()) {
                        RawResource platformResource = createPlatformResource(reportParams, platform, true);
                        rawResources.add(platformResource);
                    }
                }
            }
        }
        AiSendRuntimeReportCommandData runtimeReportData =
                    new AiSendRuntimeReportCommandData(agentToken, rawResources);
        return runtimeReportData;
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

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return EmptyAgentResponse.class;
    }

}
