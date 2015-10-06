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

package com.vmware.epops.command.downstream.mail;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.vmware.epops.command.downstream.mail.autoinventory.StartScanCommand;
import com.vmware.epops.command.downstream.mail.configuration.ConfigureCommand;
import com.vmware.epops.command.downstream.mail.control.RemovePluginCommand;
import com.vmware.epops.command.downstream.mail.management.AgentUpdateFilesCommand;
import com.vmware.epops.command.downstream.mail.management.DieCommand;
import com.vmware.epops.command.downstream.mail.management.GetCurrentBundleVersionCommand;
import com.vmware.epops.command.downstream.mail.management.PingCommand;
import com.vmware.epops.command.downstream.mail.management.RestartCommand;
import com.vmware.epops.command.downstream.mail.management.UpgradeCommand;

/**
 * Command sent by Agent Mailbox service to agents.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM,
            include = JsonTypeInfo.As.PROPERTY,
            property = "commandType")
@JsonTypeIdResolver(MailCommandResolver.class)
public interface AgentMailCommand {

    public AgentMailCommandType getCommandType();

    /**
     * @return an informative string representing the command data. it will be logged upon failure- therefore shouldn't
     *         contain any confidential information
     */
    public String getCommandDetails();

    public void setCommandDetails(String commandDetails);

}

/**
 * Used to decide which implementation of the AgentMailCommand interface we should create. The json returns:
 * "commandType": "CONFIGURE_RESOURCE" From this we assume we should create ConfigureCommand implementation.
 * 
 * @author zvikai
 * 
 */
class MailCommandResolver implements TypeIdResolver {

    private static Map<String, String> commandTypeToClass = new HashMap<>();
    private JavaType javaType;

    static {
        commandTypeToClass.put(AgentMailCommandType.START_AUTO_DISCOVERY.toString(), StartScanCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.REMOVE_PLUGIN.toString(), RemovePluginCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.PING.toString(), PingCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.RESTART.toString(), RestartCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.GET_CURRENT_BUNDLE_VERSION.toString(),
                    GetCurrentBundleVersionCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.UPGRADE.toString(), UpgradeCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.AGENT_UPDATE_FILES.toString(),
                    AgentUpdateFilesCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.DIE.toString(), DieCommand.class.getName());
        commandTypeToClass.put(AgentMailCommandType.CONFIGURE_RESOURCE.toString(), ConfigureCommand.class.getName());
    }

    @Override
    public void init(JavaType baseType) {
        javaType = baseType;
    }

    @Override
    public Id getMechanism() {
        return Id.CUSTOM;
    }

    @Override
    public String idFromValue(Object obj) {
        return idFromValueAndType(obj, obj.getClass());
    }

    @Override
    public String idFromBaseType() {
        return idFromValueAndType(null, javaType.getRawClass());
    }

    @Override
    public String idFromValueAndType(Object obj,
                                     Class<?> clazz) {
        String result = null;
        for (Map.Entry<String, String> entry : commandTypeToClass.entrySet()) {
            if (clazz.getName().equals(entry.getValue())) {
                result = entry.getKey();
            }
        }
        return result;
    }

    @Override
    public JavaType typeFromId(String type) {
        String className = commandTypeToClass.get(type);
        Class<?> foundClass;
        try {
            foundClass = ClassUtil.findClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot find class mapped to '" + type + "'");
        }
        return TypeFactory.defaultInstance().constructSpecializedType(javaType, foundClass);

    }

}
