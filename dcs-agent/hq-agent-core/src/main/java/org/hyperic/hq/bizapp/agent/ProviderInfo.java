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

package org.hyperic.hq.bizapp.agent;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.hyperic.hq.util.properties.PropertiesUtil;

public class ProviderInfo {
    private String providerAddress; // server connection string
    private String providerHost; // server IP or hostname
    private final String agentToken; // Token the agent should use to connect
    private int providerPort;

    public ProviderInfo(String providerAddress,
                        String agentToken) {
        if (providerAddress == null || agentToken == null) {
            throw new IllegalArgumentException("No arguments can be null");
        }
        validateAndInitializeAddress(providerAddress);
        this.agentToken = agentToken;
        this.providerPort = -1;
    }

    public ProviderInfo(String providerAddress,
                        String agentToken,
                        int providerPort) {
        if (providerAddress == null || agentToken == null) {
            throw new IllegalArgumentException("No arguments can be null");
        }

        validateAndInitializeAddress(providerAddress);
        PropertiesUtil.validatePort(providerPort);
        this.agentToken = agentToken;
        this.providerPort = providerPort;
    }

    /**
     * Validates an address (hostname or IP) conforms to the common standards Accepts both IPv4, IPv6 and domain names
     * 
     * @param providerAddressToSet should be either an IP or a domain name
     * @throws IllegalArgumentException if it isn't
     */
    public static void validateAddress(String hostOrIp)
        throws IllegalArgumentException {
        try {
            InetAddress.getByName(hostOrIp);
        } catch (UnknownHostException exc) {
            throw new IllegalArgumentException(exc.getMessage(), exc);
        }
    }

    private void validateAndInitializeAddress(String providerAddressToSet)
        throws IllegalArgumentException {

        URL url = null;
        try {
            url = new URL(providerAddressToSet);
            this.providerHost = url.getHost();
        } catch (MalformedURLException exc) {
            throw new IllegalArgumentException(exc.getMessage(), exc);
        }
        // continue to check validity of hostname/ip (as URL check fails to find invalid characters)
        validateAddress(url.getHost());
        this.providerAddress = providerAddressToSet;
    }

    public void setProviderPort(int port) {
        this.providerPort = port;
    }

    public int getProviderPort() {
        return this.providerPort;
    }

    public String getProviderAddress() {
        return this.providerAddress;
    }

    public String getAgentToken() {
        return this.agentToken;
    }

    public String getProviderHost() {
        return providerHost;
    }

    @Override
    public String toString() {
        return this.providerAddress;
    }
}
