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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

import com.vmware.epops.webapp.utils.HttpSenderService;

@Component("healthCheckServlet")
public class HealthCheckServlet implements HttpRequestHandler {

    public static final String RESPONSE_STATUS_ONLINE = "ONLINE";
    public static final String RESPONSE_STATUS_OFFLINE = "OFFLINE";

    private final static Logger logger = LoggerFactory
                .getLogger(HealthCheckServlet.class);

    @Autowired
    private HttpSenderService httpSenderService;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "REC_CATCH_EXCEPTION",
                justification = "catch all kind of exceptions and return unavailable")
    @Override
    public void handleRequest(HttpServletRequest req,
                              HttpServletResponse res)
        throws ServletException, IOException {

        try {
            ResponseEntity<String> check = httpSenderService.healthCheck();
            if (HttpStatus.OK.equals(check.getStatusCode())) {
                res.setStatus(HttpStatus.OK.value());
                res.getWriter().print(RESPONSE_STATUS_ONLINE);
            } else {
                res.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                res.getWriter().print(RESPONSE_STATUS_OFFLINE);
            }
            if (logger.isDebugEnabled()) {
                logger.debug(String
                            .format("Received a health check call. Agent adapter returned %s - %s",
                                        check.getStatusCode(), check.getBody()));
            }
        } catch (Exception e) {
            logger.error("Exception while calling agent adapter to get health-check", e);
            res.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            res.getWriter().print(RESPONSE_STATUS_OFFLINE);
            logger.debug(String
                        .format("Received a health check call. Agent adapter failed with exception %s. Returning 503.",
                                    e.getMessage()));
        }
    }

}
