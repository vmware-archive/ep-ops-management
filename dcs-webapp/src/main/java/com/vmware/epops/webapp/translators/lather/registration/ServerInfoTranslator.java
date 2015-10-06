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

package com.vmware.epops.webapp.translators.lather.registration;

import org.hyperic.hq.bizapp.shared.lather.ServerInfo_result;
import org.hyperic.lather.LatherValue;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.upstream.registration.ServerInfoCommandData;
import com.vmware.epops.command.upstream.registration.ServerInfoCommandResponse;

public class ServerInfoTranslator implements LatherRegistrationCommandTranslator {

    @Override
    public AgentCommandData translateRequest(LatherValue latherValue) {
        return new ServerInfoCommandData();
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        if (response == null) {
            return null;
        }
        if (!(response instanceof ServerInfoCommandResponse)) {
            throw new IllegalArgumentException("No response to server info command to translate");
        }
        ServerInfoCommandResponse serverInfoCommandResponse = (ServerInfoCommandResponse) response;
        String isCustomCertificate = Boolean.toString(serverInfoCommandResponse.isCustomCertificate());

        ServerInfo_result serverInfoResult = new ServerInfo_result();
        serverInfoResult.setIsCustomCertificate(isCustomCertificate);
        return serverInfoResult;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return ServerInfoCommandResponse.class;
    }

}
