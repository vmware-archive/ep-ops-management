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

package com.vmware.epops.webapp.utils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.AgentCommandResponse;

@Service
public class HttpSenderService {

    @Autowired
    private final RestTemplate restTemplate = null;
    private final String host;
    private final int port;

    @Autowired
    public HttpSenderService(@Value("${server.address}") String host,
                             @Value("${server.port}") String port) {

        this.port = Integer.parseInt(port);
        this.host = host;
    }

    public <T extends AgentCommandResponse> T sendCommand(AgentCommandData agentCommandData,
                                                          Class<T> responseType)
        throws MalformedURLException, URISyntaxException {

        String file = "/agentAdapterCommand/" + agentCommandData.getClass().getSimpleName();
        URL url = new URL("http", host, port, file);

        return restTemplate.postForObject(url.toURI(), agentCommandData, responseType);
    }

    public ResponseEntity<String> healthCheck()
        throws MalformedURLException, URISyntaxException {

        URL url = new URL("http", host, port, "/health-check");
        return restTemplate.getForEntity(url.toURI(), String.class);
    }
}
