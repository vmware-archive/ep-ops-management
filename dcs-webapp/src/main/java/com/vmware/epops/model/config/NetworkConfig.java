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

package com.vmware.epops.model.config;

import java.util.Formatter;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class NetworkConfig {

    private String ip;
    private String netmask;
    private String mac;
    private final static String NETWORK_CONFIG_STRING_FORMAT = "[%s, %s, %s]"; // [ip , netmask , mac]

    // delimiter
    public NetworkConfig() {
        super();
    }

    public NetworkConfig(String ip,
                         String netmask,
                         String mac) {
        super();
        this.ip = ip;
        this.netmask = netmask;
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof NetworkConfig)) {
            return false;
        }
        final NetworkConfig nc = (NetworkConfig) obj;
        final EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(ip, nc.ip);
        equalsBuilder.append(netmask, nc.netmask);
        equalsBuilder.append(mac, nc.mac);

        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(ip);
        hashCodeBuilder.append(netmask);
        hashCodeBuilder.append(mac);
        return hashCodeBuilder.toHashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     * In Format [ip , netmask , mac]
     */
    @Override
    public String toString() {

        Formatter formatter = new Formatter();
        String string = formatter.format(NETWORK_CONFIG_STRING_FORMAT, ip, netmask, mac).toString();
        formatter.close();
        return string;

    }

}
