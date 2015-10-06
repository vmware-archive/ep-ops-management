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

import com.vmware.epops.command.AgentCommandData;

public class VerifyCommandData implements AgentCommandData {

    private String certificateSerialNumber;
    private String agentToken;
    private String agentIp;
    private String userName;
    private String password;
    private String commandName;

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
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

    public String getAgentToken() {
        return agentToken;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String getCommandName() {
        commandName = "VerifyAgent";
        return commandName;
    }

    public String getCertificateSerialNumber() {
        return certificateSerialNumber;
    }

    public void setCertificateSerialNumber(String certificateSerialNumber) {
        this.certificateSerialNumber = certificateSerialNumber;
    }

}
