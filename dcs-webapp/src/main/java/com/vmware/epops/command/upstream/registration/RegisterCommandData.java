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

package com.vmware.epops.command.upstream.registration;

import org.apache.commons.lang.ArrayUtils;

import com.vmware.epops.command.AgentCommandData;

public class RegisterCommandData implements AgentCommandData {

    private String version;
    private int cpuCount;
    private byte[] certificateRequest;
    private String agentIp;
    private String userName;
    private String password;

    public String getVersion() {
        return this.version;
    }

    public int getCpuCount() {
        return this.cpuCount;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCpuCount(int cpuCount) {
        this.cpuCount = cpuCount;
    }

    @Override
    public String getCommandName() {
        return "registerAgent";
    }

    public byte[] getCertificateRequest() {
        return ArrayUtils.clone(this.certificateRequest);
    }

    public void setCertificateRequest(byte[] csr) {
        this.certificateRequest = ArrayUtils.clone(csr);
    }

    public String getAgentIp() {
        return agentIp;
    }

    public void setAgentIp(String agentIp) {
        this.agentIp = agentIp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
