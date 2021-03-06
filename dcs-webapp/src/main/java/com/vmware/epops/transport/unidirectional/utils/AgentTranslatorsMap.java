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

package com.vmware.epops.transport.unidirectional.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.webapp.translators.agent.AgentCommandTranslator;

@Component
public class AgentTranslatorsMap {

    private final Map<AgentMailCommandType, AgentCommandTranslator> translatorsMap =
                new HashMap<AgentMailCommandType, AgentCommandTranslator>();

    public void registerTranslator(AgentMailCommandType commandType,
                                   AgentCommandTranslator translator) {
        translatorsMap.put(commandType, translator);
    }

    public AgentCommandTranslator getTranslator(AgentMailCommandType commandType) {
        return translatorsMap.get(commandType);
    }
}
