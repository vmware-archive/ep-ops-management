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

package org.hyperic.util.http;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.hyperic.util.http.AgentRequest.AgentHttpMethod;
import org.hyperic.util.security.CertificateService;
import org.hyperic.util.security.KeystoreConfig;

public class ServerHttpClient extends HQHttpClient {

    private final X500Principal userToken;
    protected final int maxRequestsPerConnection;
    protected volatile int connectionCounter;
    protected ServersManager serversManager;

    public ServerHttpClient(final KeystoreConfig keyConfig,
                            final HttpConfig config,
                            final boolean acceptUnverifiedCertificates,
                            final ClientConnectionManager conman,
                            int maxRequestsPerConnection,
                            boolean isSupportRRDNS,
                            int failPeriodInMin,
                            int downPeriodInMin) {

        super(keyConfig, config, acceptUnverifiedCertificates, conman);
        this.maxRequestsPerConnection = maxRequestsPerConnection;

        userToken = CertificateService
                    .getClientCertificateSubjectPrincipal(keyConfig);
        connectionCounter = 0;
        CommunicationConfiguration communicationConfiguration =
                    new CommunicationConfiguration(isSupportRRDNS, failPeriodInMin, downPeriodInMin);
        serversManager = new ServersManager(this, communicationConfiguration);

    }

    public ServerHttpClient(final KeystoreConfig keyConfig,
                            final HttpConfig config,
                            final boolean acceptUnverifiedCertificates,
                            int maxRequestsPerConnection,
                            boolean isSupportRRDNS,
                            int failPeriodInMin,
                            int downPeriodInMin) {
        this(keyConfig, config, acceptUnverifiedCertificates, null,
                    maxRequestsPerConnection, isSupportRRDNS, failPeriodInMin, downPeriodInMin);
    }

    @Override
    protected HttpContext createHttpContext() {
        HttpContext httpContext = super.createHttpContext();
        if (userToken != null) {
            httpContext.setAttribute(ClientContext.USER_TOKEN, userToken);
        }

        return httpContext;
    }

    public HttpResponse post(String url,
                             Map<String, String> params,
                             boolean closeConn)
        throws ClientProtocolException, IOException {
        AgentRequest request = new AgentRequest(url, AgentHttpMethod.POST);
        request.setParams(params);
        return this.send(request, closeConn);

    }

    public HttpResponse get(String url,
                            boolean closeConn)
        throws ClientProtocolException, IOException {
        AgentRequest request = new AgentRequest(url, AgentHttpMethod.GET);
        return this.send(request, closeConn);
    }

    private HttpResponse send(AgentRequest request,
                              boolean closeConn)
        throws ClientProtocolException, IOException {
        if (closeConn || (connectionCounter >= maxRequestsPerConnection)) {
            connectionCounter = 0;
            request.getHeaders().put(HttpHeaders.CONNECTION, "close");
        } else {
            connectionCounter++;
        }
        return serversManager.send(request);
    }

}
