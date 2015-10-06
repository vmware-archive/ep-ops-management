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

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

import com.vmware.epops.webapp.servlets.lather.LatherRegistrationRequestHandler;
import com.vmware.epops.webapp.translators.lather.registration.LatherRegistrationCommandTranslatorFactory;
import com.vmware.epops.webapp.translators.lather.registration.RegisterAgentTranslator;
import com.vmware.epops.webapp.translators.lather.registration.ServerInfoTranslator;

@Component("agentRegistrationServlet")
public class AgentRegistrationServlet implements HttpRequestHandler {

    // private final static Logger logger = LoggerFactory.getLogger(AgentRegistrationServlet.class);

    private final static String SERVER_INFO_LATHER_METHOD = "serverInfo";
    private final static String REGISTER_AGENT_LATHER_METHOD = "registerAgent";

    @Autowired
    private LatherRegistrationRequestHandler latherRegistrationRequestHandler;

    @PostConstruct
    public void init()
        throws ServletException {
        LatherRegistrationCommandTranslatorFactory.INSTANCE.registerTranslator(SERVER_INFO_LATHER_METHOD,
                    new ServerInfoTranslator());
        LatherRegistrationCommandTranslatorFactory.INSTANCE.registerTranslator(REGISTER_AGENT_LATHER_METHOD,
                    new RegisterAgentTranslator());
    }

    @Override
    public void handleRequest(HttpServletRequest req,
                              HttpServletResponse resp)
        throws ServletException, IOException {
        latherRegistrationRequestHandler.handleRequest(req, resp, null);
    }
}
