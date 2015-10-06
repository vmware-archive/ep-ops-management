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

package com.vmware.epops.webapp.servlets;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.upstream.registration.VerifyAgentCommandResponse;
import com.vmware.epops.command.upstream.registration.VerifyCommandData;
import com.vmware.epops.transport.unidirectional.service.AgentCommandQueueService;
import com.vmware.epops.util.security.CertificateHandler;
import com.vmware.epops.webapp.servlets.lather.LatherRequestHandler;
import com.vmware.epops.webapp.translators.lather.AgentVerifiedLatherCommandTranslatorFactory;
import com.vmware.epops.webapp.translators.lather.AiSendReportTranslator;
import com.vmware.epops.webapp.translators.lather.AiSendRuntimeReportTranslator;
import com.vmware.epops.webapp.translators.lather.GetAgentCommandsCommandTranslator;
import com.vmware.epops.webapp.translators.lather.GetDisabledPluginsCommandTranslator;
import com.vmware.epops.webapp.translators.lather.MeasurementSendReportTranslator;
import com.vmware.epops.webapp.translators.lather.PluginSendReportTranslator;
import com.vmware.epops.webapp.translators.lather.SendAgentResponseCommandTranslator;
import com.vmware.epops.webapp.utils.HttpSenderService;

@Component("agentServlet")
public class AgentServlet implements HttpRequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(AgentServlet.class);
    private final static String UNAUTHORIZED_MSG = "Unauthorized agent denied";

    @Autowired
    private LatherRequestHandler latherRequestHandler;
    @Autowired
    private CertificateHandler certificateHandler;
    @Autowired
    private AgentCommandQueueService agentCommandQueueService;
    @Autowired
    private HttpSenderService httpSenderService;

    @PostConstruct
    public void init()
        throws ServletException {
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_AI_SEND_REPORT,
                                new AiSendReportTranslator());
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_AI_SEND_RUNTIME_REPORT,
                                new AiSendRuntimeReportTranslator());
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_MEASUREMENT_SEND_REPORT,
                                new MeasurementSendReportTranslator());
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_GET_AGENT_COMMANDS,
                                new GetAgentCommandsCommandTranslator(agentCommandQueueService));
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_SEND_RESPONSE,
                                new SendAgentResponseCommandTranslator(agentCommandQueueService));
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_PLUGIN_SEND_REPORT,
                                new PluginSendReportTranslator());
        AgentVerifiedLatherCommandTranslatorFactory.INSTANCE
                    .registerTranslator(CommandInfo.CMD_GET_DISABLED_PLUGINS,
                                new GetDisabledPluginsCommandTranslator());
    }

    @Override
    public void handleRequest(HttpServletRequest req,
                              HttpServletResponse resp)
        throws ServletException, IOException {
        VerifyCommandData agentCommandData;
        try {
            agentCommandData = getSerialAndTokenFromRequest(req);
        } catch (CertificateEncodingException ce) {
            logger.error("Problem encoding certificate", ce);
            throw new ServletException(ce);
        }

        if (agentCommandData == null) {
            latherRequestHandler.issueErrorResponse(resp, UNAUTHORIZED_MSG);
            return;
        }

        AgentCommandResponse verifyAgentResponse = null;
        try {
            verifyAgentResponse = httpSenderService.sendCommand(agentCommandData, VerifyAgentCommandResponse.class);
        } catch (Exception e) {
            logger.error("Getting error when calling with VerifyCommandData", e);
            // If we cannot connect to the adapter, then we assume its not available,
            // thus we notify the agent so it would try to connect to the next node
            // in the cluster. Note: when we handle the request (down this flow)
            // we try to connect to the adapter once again. This time we do not
            // return 503 if the connection failed, aince we already checked it here.
            // If this call (for some reason) is removed, we need to make sure we return
            // 503 in case adapter is down.
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
            return;
        }

        String agentToken = ((VerifyAgentCommandResponse) verifyAgentResponse).getToken();
        if (StringUtils.isNotEmpty(agentToken)) {
            latherRequestHandler.handleRequest(req, resp, agentToken);
        } else {
            latherRequestHandler.issueErrorResponse(resp, UNAUTHORIZED_MSG);
        }
    }

    private VerifyCommandData getSerialAndTokenFromRequest(HttpServletRequest request)
        throws CertificateEncodingException {
        String agentToken = certificateHandler.extractAgentTokenFromRequest(request);
        if (StringUtils.isBlank(agentToken)) {
            logger.error(request.getRemoteAddr() + ": " + UNAUTHORIZED_MSG + ". token:"
                        + StringUtils.defaultIfEmpty(agentToken, "(empty)"));
            return null;
        }

        String clientCertificateSerialNumber = certificateHandler.extractCertificateSerialNumberFromRequest(request);
        if (StringUtils.isBlank(clientCertificateSerialNumber)) {
            logger.error(request.getRemoteAddr() + ": " + UNAUTHORIZED_MSG + ". token:"
                        + StringUtils.defaultIfEmpty(agentToken, "(empty)"));
            return null;
        }

        VerifyCommandData verifyCommandData = new VerifyCommandData();
        verifyCommandData.setAgentToken(agentToken);
        verifyCommandData.setCertificateSerialNumber(clientCertificateSerialNumber);
        return verifyCommandData;
    }
}
