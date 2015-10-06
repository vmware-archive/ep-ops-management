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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.hyperic.hq.agent.FileMetadata;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.epops.command.downstream.mail.AgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.command.downstream.mail.AgentMailResponse;
import com.vmware.epops.command.downstream.mail.IllegalCommandException;
import com.vmware.epops.command.downstream.mail.IllegalCommandException.CommandError;
import com.vmware.epops.command.downstream.mail.management.AgentManagementCommand;
import com.vmware.epops.command.downstream.mail.management.AgentUpdateFilesCommand;
import com.vmware.epops.command.downstream.mail.management.DieCommand;
import com.vmware.epops.command.downstream.mail.management.GetCurrentBundleVersionCommand;
import com.vmware.epops.command.downstream.mail.management.PingCommand;
import com.vmware.epops.command.downstream.mail.management.PingResponse;
import com.vmware.epops.command.downstream.mail.management.RestartCommand;
import com.vmware.epops.command.downstream.mail.management.UpgradeCommand;
import com.vmware.epops.transport.unidirectional.proxies.AgentProxy;
import com.vmware.epops.transport.unidirectional.utils.AgentTranslatorsMap;
import com.vmware.epops.webapp.translators.agent.AgentCommandTranslator;

/*
 * translator from AgentManagementCommand to InvocationRequest
 */
@Component
public class AgentManagementCommandsTranslator implements AgentCommandTranslator {

    private final static Logger log = LoggerFactory.getLogger(AgentManagementCommandsTranslator.class);

    @Autowired
    private AgentTranslatorsMap agentTranslatorsMap;
    private AgentProxy agentManagementProxy;

    @PostConstruct
    public void init() {
        agentManagementProxy = new AgentProxy(AgentCommandsClient.class);
        agentTranslatorsMap.registerTranslator(PingCommand.COMMAND_TYPE, this);
        agentTranslatorsMap.registerTranslator(RestartCommand.COMMAND_TYPE, this);
        agentTranslatorsMap.registerTranslator(DieCommand.COMMAND_TYPE, this);
        agentTranslatorsMap.registerTranslator(GetCurrentBundleVersionCommand.COMMAND_TYPE, this);
        agentTranslatorsMap.registerTranslator(UpgradeCommand.COMMAND_TYPE, this);
        agentTranslatorsMap.registerTranslator(AgentUpdateFilesCommand.COMMAND_TYPE, this);
    }

    @Override
    public InvocationRequest translateCommand(AgentMailCommand agentMailCommand)
        throws Throwable {
        if (!(agentMailCommand instanceof AgentManagementCommand)) {
            throw new IllegalCommandException(CommandError.COMMAND_NOT_SUPPORTED, agentMailCommand.getCommandDetails(),
                        this.getClass().getSimpleName());
        }
        return translateCommand((AgentManagementCommand) agentMailCommand);
    }

    private InvocationRequest translateCommand(AgentManagementCommand agentMailCommand)
        throws Throwable { // NOPMD
        log.debug("translating command:" + agentMailCommand.getCommandType());
        if (agentMailCommand instanceof PingCommand) {
            return agentManagementProxy.getInvocationRequest("ping");
        } else if (agentMailCommand instanceof RestartCommand) {
            return agentManagementProxy.getInvocationRequest("restart");
        } else if (agentMailCommand instanceof DieCommand) {
            return agentManagementProxy.getInvocationRequest("die");
        } else if (agentMailCommand instanceof GetCurrentBundleVersionCommand) {
            return agentManagementProxy.getInvocationRequest("getCurrentAgentBundle");
        } else if (agentMailCommand instanceof UpgradeCommand) {
            UpgradeCommand upgradeCommand = (UpgradeCommand) agentMailCommand;
            Class<?>[] parameterTypes = new Class<?>[] { String.class, String.class };
            Object[] args = new Object[] { upgradeCommand.getTarFile(), upgradeCommand.getDestination() };
            return agentManagementProxy.getInvocationRequest("upgrade", parameterTypes, args);
        } else if (agentMailCommand instanceof AgentUpdateFilesCommand) {
            AgentUpdateFilesCommand agentUpdateFilesCommand = (AgentUpdateFilesCommand) agentMailCommand;
            Class<?>[] parameterTypes =
                        new Class<?>[] { FileMetadata[].class, String[].class, Boolean.class };

            Object[] args =
                        new Object[] {
                                    convertServerMetadataToAgentMetadata(agentUpdateFilesCommand.getFilesToUpdate()),
                                    agentUpdateFilesCommand.getFilesToRemove(),
                                    agentUpdateFilesCommand.isRestartIfSuccessful() };
            return agentManagementProxy.getInvocationRequest("agentUpdateFiles", parameterTypes, args);
        } else {
            throw new IllegalCommandException(CommandError.COMMAND_NOT_IMPLEMENTED,
                        agentMailCommand.getCommandDetails(), this.getClass().getSimpleName());
        }
    }

    @Override
    public AgentMailResponse translateResponse(AgentMailCommandType commandType,
                                               InvocationResponse response) {

        switch (commandType) {
            case PING:
                return new PingResponse((long) response.getPayload());
            case RESTART:
            case DIE:
            case GET_CURRENT_BUNDLE_VERSION:
            case UPGRADE:
            case AGENT_UPDATE_FILES:
                return null;
            default:
                throw new IllegalArgumentException("response for " + commandType
                            + " is not implemented");
        }
    }

    private FileMetadata[] convertServerMetadataToAgentMetadata(AgentUpdateFilesCommand.FileMetadata[] files) {
        if (null == files) {
            return (new FileMetadata[0]);
        }

        List<FileMetadata> agentMetaDataList = new ArrayList<FileMetadata>(files.length);

        for (AgentUpdateFilesCommand.FileMetadata file : files) {
            agentMetaDataList.add(new FileMetadata(file.getSourceFileURI(),
                        file.getDestFileRelativePath(), file.getMd5sum()));
        }
        return agentMetaDataList.toArray(new FileMetadata[agentMetaDataList.size()]);
    }
}
