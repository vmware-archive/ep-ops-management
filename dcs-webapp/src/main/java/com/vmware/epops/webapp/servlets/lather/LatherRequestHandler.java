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

package com.vmware.epops.webapp.servlets.lather;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.client.LatherHTTPClient;
import org.hyperic.lather.xcode.LatherXCoder;
import org.hyperic.util.encoding.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.webapp.translators.lather.AgentVerifiedLatherCommandTranslator;
import com.vmware.epops.webapp.translators.lather.AgentVerifiedLatherCommandTranslatorFactory;
import com.vmware.epops.webapp.translators.lather.LatherCommandTranslator;
import com.vmware.epops.webapp.utils.HttpSenderService;

@Component
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "REC_CATCH_EXCEPTION", justification = "All exceptions are cought")
public class LatherRequestHandler {
    private final static Logger logger = LoggerFactory
                .getLogger(LatherRequestHandler.class);

    @Autowired
    private HttpSenderService httpSenderService;

    public void handleRequest(HttpServletRequest req,
                              HttpServletResponse resp,
                              String agentToken)
        throws ServletException, IOException {
        LatherXCoder xCoder;
        LatherValue val;
        String[] method;
        String[] args;
        String[] argsClass;
        LatherContext ctx;
        byte[] decodedArgs;
        Class<?> valClass;

        ctx = new LatherContext();
        ctx.setCallerIP(req.getRemoteAddr());
        ctx.setRequestTime(System.currentTimeMillis());

        xCoder = new LatherXCoder();

        method = req.getParameterValues("method");
        args = req.getParameterValues("args");
        argsClass = req.getParameterValues("argsClass");

        if (method == null || args == null || argsClass == null
                    || method.length != 1 || args.length != 1
                    || argsClass.length != 1) {
            String msg = "Invalid Lather request made from "
                        + req.getRemoteAddr() + " agent: " + agentToken;
            logger.error(msg);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        try {
            valClass = Class.forName(argsClass[0], true, xCoder.getClass()
                        .getClassLoader());
        } catch (ClassNotFoundException exc) {
            String msg = "Lather request from " + req.getRemoteAddr()
                        + " agent: " + agentToken
                        + " required an argument object of class '" + argsClass[0]
                        + "' which could not be found";
            logger.error(msg);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        decodedArgs = Base64.decode(args[0]);
        try (DataInputStream dIs = new DataInputStream(new ByteArrayInputStream(decodedArgs))) {

            val = xCoder.decode(dIs, valClass);
        } catch (Exception exc) {
            logger.error("failed to decode args." + " (agent:" + agentToken + ") ", exc);
            issueErrorResponse(resp, exc.toString());
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Handling {} command request from agent:{}", method, agentToken);
        }
        handleMethod(resp, xCoder, val, method, agentToken);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "REC_CATCH_EXCEPTION",
                justification = "Exceptions are caught to ensure that an error response is returned to the caller")
    private void handleMethod(HttpServletResponse resp,
                              LatherXCoder xCoder,
                              LatherValue val,
                              String[] method,
                              String agentToken)
        throws IOException {
        try {
            AgentCommandResponse response = null;
            LatherCommandTranslator commandDataTranslator = getCommandDataTranslator(method[0]);
            AgentCommandData agentCommandData = getAgentCommandData(commandDataTranslator, val, agentToken, method[0]);

            if (null != agentCommandData) {
                response = httpSenderService.sendCommand(agentCommandData, commandDataTranslator.getResponseType());
                LatherValue translatedResponse = commandDataTranslator
                            .translateResponse(response);

                issueSuccessResponse(resp, xCoder, translatedResponse);
            } else {
                String errorMessage = "Failed to translate agent command data "
                            + method[0] + " (agent:" + agentToken + ") ";
                logger.error(errorMessage);
                issueErrorResponse(resp, errorMessage);
            }
        } catch (Exception e) {
            logger.error("Exception for method: " + method[0] + " (agent:" + agentToken + ") ", e);
            issueErrorResponse(resp, e.getMessage());
        }
    }

    protected LatherCommandTranslator getCommandDataTranslator(String commandName) {
        AgentVerifiedLatherCommandTranslator translator =
                    AgentVerifiedLatherCommandTranslatorFactory.INSTANCE.getTranslator(commandName);
        if (translator == null) {
            throw new IllegalArgumentException("No translator found for method: " + commandName);
        }
        return translator;
    }

    protected AgentCommandData getAgentCommandData(LatherCommandTranslator commandDataTranslator,
                                                   LatherValue latherValue,
                                                   String agentToken,
                                                   String commandName) {
        AgentVerifiedCommandData agentVerifiedCommandData = null;
        if (commandDataTranslator instanceof AgentVerifiedLatherCommandTranslator) {
            AgentVerifiedLatherCommandTranslator verifiedLatherCommandTranslator =
                        (AgentVerifiedLatherCommandTranslator) commandDataTranslator;
            agentVerifiedCommandData = verifiedLatherCommandTranslator.translateRequest(latherValue, agentToken);
        } else { // this should never happen as the translator is a map by the commandName
            throwCommmandsMismatch(commandName, commandDataTranslator);
        }
        return agentVerifiedCommandData;
    }

    protected void throwCommmandsMismatch(String commandName,
                                          LatherCommandTranslator commandDataTranslator) {
        throw new RuntimeException("translator " + commandDataTranslator.getClass().getSimpleName()
                    + " doesn't match the command: " + commandName);
    }

    public void issueSuccessResponse(HttpServletResponse resp,
                                     LatherXCoder xCoder,
                                     LatherValue res)
        throws IOException {
        byte[] rawData;

        resp.setContentType("text/latherValue");
        resp.setHeader(LatherHTTPClient.HDR_VALUECLASS, res.getClass()
                    .getName());

        try (ByteArrayOutputStream bOs = new ByteArrayOutputStream();
                    DataOutputStream dOs = new DataOutputStream(bOs)) {
            xCoder.encode(res, dOs);
            rawData = bOs.toByteArray();
            resp.getOutputStream().print(Base64.encode(rawData));
        }
    }

    public void issueErrorResponse(HttpServletResponse resp,
                                   String errMsg)
        throws IOException {
        resp.setContentType("text/raw");
        resp.setIntHeader(LatherHTTPClient.HDR_ERROR, 1);
        resp.getOutputStream().print(errMsg);
    }

    public AgentCommandResponse handleVerifiedRequest(HttpServletRequest req) {
        return null;
    }
}
