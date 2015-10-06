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

package com.vmware.epops.webapp.servlets.lather;

import org.hyperic.lather.LatherValue;
import org.springframework.stereotype.Component;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.webapp.translators.lather.LatherCommandTranslator;
import com.vmware.epops.webapp.translators.lather.registration.LatherRegistrationCommandTranslator;
import com.vmware.epops.webapp.translators.lather.registration.LatherRegistrationCommandTranslatorFactory;

@Component
public class LatherRegistrationRequestHandler extends LatherRequestHandler {

    @Override
    protected LatherCommandTranslator getCommandDataTranslator(String commandName) {
        LatherRegistrationCommandTranslator translator =
                    LatherRegistrationCommandTranslatorFactory.INSTANCE.getTranslator(commandName);
        if (translator == null) {
            throw new IllegalArgumentException("No translator found for method: " + commandName);
        }
        return translator;
    }

    @Override
    /**
    this method will ignore the agentToken arg by calling its own implementation
     */
    protected AgentCommandData getAgentCommandData(LatherCommandTranslator commandDataTranslator,
                                                   LatherValue latherValue,
                                                   String agentToken,
                                                   String commandName) {
        return getAgentCommandData(commandDataTranslator, latherValue, commandName);
    }

    private AgentCommandData getAgentCommandData(LatherCommandTranslator commandDataTranslator,
                                                 LatherValue latherValue,
                                                 String commandName) {
        AgentCommandData agentCommandData = null;
        if (commandDataTranslator instanceof LatherRegistrationCommandTranslator) {
            LatherRegistrationCommandTranslator registrationCommandTranslator =
                        (LatherRegistrationCommandTranslator) commandDataTranslator;
            agentCommandData = registrationCommandTranslator.translateRequest(latherValue);
        } else { // this should never happen as the translator is a map by the commandName
            throwCommmandsMismatch(commandName, commandDataTranslator);
        }
        return agentCommandData;
    }

}
