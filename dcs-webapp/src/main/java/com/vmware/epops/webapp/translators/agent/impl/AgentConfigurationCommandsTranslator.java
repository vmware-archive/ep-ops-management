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

package com.vmware.epops.webapp.translators.agent.impl;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.hq.configuration.agent.client.ConfigurationCommandsClient;
import org.hyperic.hq.configuration.agent.commands.Configuration_args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;
import com.vmware.epops.command.downstream.mail.IllegalCommandException;
import com.vmware.epops.command.downstream.mail.IllegalCommandException.CommandError;
import com.vmware.epops.command.downstream.mail.configuration.ConfigureCommand;
import com.vmware.epops.command.downstream.mail.configuration.MetricScheduling;
import com.vmware.epops.plugin.converter.NameMappingUtil;
import com.vmware.epops.transport.unidirectional.proxies.AgentProxy;
import com.vmware.epops.transport.unidirectional.utils.AgentTranslatorsMap;
import com.vmware.epops.webapp.translators.agent.AgentCommandTranslator;

/**
 * Translating ConfigureCommand to InvocationRequest
 * 
 * @author rina
 */
@Component
public class AgentConfigurationCommandsTranslator implements AgentCommandTranslator {
    private final static Logger logger = LoggerFactory.getLogger(AgentConfigurationCommandsTranslator.class);
    private final static String CONFIGURATION_METHOD_NAME = "configure";
    private final static String PARENT_ID = "parentID";
    private final static String MONITORED_RESOURCE_ID = "monitoredResourceID";
    private AgentProxy agentResourceConfigurationProxy;
    @Autowired
    private AgentTranslatorsMap agentTranslatorsMap;

    @PostConstruct
    public void init() {
        agentResourceConfigurationProxy = new AgentProxy(ConfigurationCommandsClient.class);
        agentTranslatorsMap.registerTranslator(AgentMailCommandType.CONFIGURE_RESOURCE, this);
    }

    @Override
    public InvocationRequest translateCommand(AgentMailCommand agentMailCommand)
        throws Throwable {
        ConfigureCommand configureCommand = getAsConfigureCommand(agentMailCommand);

        return translateCommand(configureCommand);
    }

    private ConfigureCommand getAsConfigureCommand(AgentMailCommand agentMailCommand) {
        if (agentMailCommand instanceof ConfigureCommand) {
            return (ConfigureCommand) agentMailCommand;
        }

        throw new IllegalCommandException(CommandError.COMMAND_NOT_SUPPORTED, agentMailCommand.getCommandDetails(),
                    this.getClass().getSimpleName());
    }

    /**
     * Translating ConfigureCommand to InvocationRequest. Configuration_args is being built from ConfigureCommand, and
     * being passed to the agent within the invocation request.
     */
    private InvocationRequest translateCommand(ConfigureCommand command)
        throws Throwable {
        logger.debug("Translating command {} ", command.getCommandType());

        Class<?>[] types = { Configuration_args.class };
        Configuration_args configurationArgs = createConfigurationArgs(command);
        Object[] args = new Object[] { configurationArgs };
        InvocationRequest invocationRequest =
                    agentResourceConfigurationProxy.getInvocationRequest(CONFIGURATION_METHOD_NAME, types, args);

        logger.debug("Finish translating {} ", command.getCommandType());
        return invocationRequest;
    }

    private Configuration_args createConfigurationArgs(ConfigureCommand command) {

        Configuration_args configurationArgs = new Configuration_args();
        Map<String, String> commandConfiguration = command.getConfiguration();

        configurationArgs.setResourceInternalId(command.getResourceId());
        configurationArgs.setMonitoredResourceId(commandConfiguration.get(MONITORED_RESOURCE_ID));
        configurationArgs.setParentId(commandConfiguration.get(PARENT_ID));
        configurationArgs.setResourceKind(NameMappingUtil.convertResourceTypeName(command.getResourceKind()));
        configurationArgs.setIsDeleteResource(command.isDeleteResource());
        if (command.containsSecuredConfiguration()) {
            configurationArgs.setSecuredConfiguration(command.getSecuredConfigurationDecrypted());
            configurationArgs.setConfiguration(command.getPublicConfiguration());
        } else {
            configurationArgs.setConfiguration(commandConfiguration);
        }
        Map<String, String> convertedProperties = NameMappingUtil.convertProperties(command.getProperties());
        configurationArgs.setProperties(convertedProperties);
        for (MetricScheduling metric : command.getEnableMetrics()) {
            configurationArgs.addScheduling(NameMappingUtil.convertMetricName(metric.getMetricName()),
                        Integer.toString(metric.getMetricId()),
                        metric.getPollingInterval().toString());
        }
        return configurationArgs;
    }

    @Override
    public AgentMailResponse translateResponse(AgentMailCommandType commandType,
                                               InvocationResponse response) {
        return null;
    }

}
