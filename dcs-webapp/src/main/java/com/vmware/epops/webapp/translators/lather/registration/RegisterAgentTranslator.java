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

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_result;
import org.hyperic.lather.LatherValue;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.upstream.registration.RegisterAgentCommandResponse;
import com.vmware.epops.command.upstream.registration.RegisterCommandData;

public class RegisterAgentTranslator implements LatherRegistrationCommandTranslator {

    @Override
    public AgentCommandData translateRequest(LatherValue latherValue) {
        if (!(latherValue instanceof RegisterAgent_args)) {
            return null;
        }
        RegisterAgent_args latherArgs = (RegisterAgent_args) latherValue;
        RegisterCommandData commandData = new RegisterCommandData();
        commandData.setAgentIp(latherArgs.getAgentIP());
        String userName = latherArgs.getUser();
        String password = latherArgs.getPword();
        commandData.setUserName(userName);
        commandData.setPassword(password);
        commandData.setCpuCount(latherArgs.getCpuCount());
        commandData.setVersion(latherArgs.getVersion());
        byte[] certificateRequest = latherArgs.getCertificateRequest();
        // the encoding is needed for json serialization
        commandData.setCertificateRequest(Base64.encode(certificateRequest));
        return commandData;
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        if (!(response instanceof RegisterAgentCommandResponse)) {
            throw new IllegalArgumentException("No response to registerAgent command to translate");
        }
        RegisterAgentCommandResponse agentCommandResponse = (RegisterAgentCommandResponse) response;
        String certificate = agentCommandResponse.getCertificate();
        String errorMsg = agentCommandResponse.getErrorString();

        if (StringUtils.isEmpty(errorMsg) && StringUtils.isEmpty(certificate)) {
            throw new RuntimeException("Invalid server response");
        }

        RegisterAgent_result registerAgentResult = new RegisterAgent_result();
        // We must set both fields with values (at least with empty ones), as LatherValue doesn't store null values.
        registerAgentResult.setErrorMessage(convertToValidLatherStringValue(errorMsg));
        registerAgentResult.setCertificate(convertToValidLatherStringValue(certificate));
        return registerAgentResult;
    }

    /**
     * Converts null strings (and also empty ones) to empty ones, as LatherValue does not accept null values.
     * 
     * @param toNormalize
     * @return
     */
    private String convertToValidLatherStringValue(String toNormalize) {
        return StringUtils.isEmpty(toNormalize) ? StringUtils.EMPTY : toNormalize;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return RegisterAgentCommandResponse.class;
    }
}
