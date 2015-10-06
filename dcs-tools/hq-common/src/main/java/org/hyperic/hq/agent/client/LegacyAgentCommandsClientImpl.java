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

package org.hyperic.hq.agent.client;

import java.util.Collection;
import java.util.Map;

import org.hyperic.hq.agent.AgentCommandsAPI;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.FileMetadata;
import org.hyperic.hq.agent.commands.AgentBundle_args;
import org.hyperic.hq.agent.commands.AgentBundle_result;
import org.hyperic.hq.agent.commands.AgentDie_args;
import org.hyperic.hq.agent.commands.AgentPing_args;
import org.hyperic.hq.agent.commands.AgentRestart_args;
import org.hyperic.hq.agent.commands.AgentUpdateFiles_result;

/**
 * The set of commands a client can call to a remote agent. This object provides a specific API which wraps the generic
 * functions which a remote agent implements.
 * 
 * 12/2014: This is the set of commands that can be called from AgentClient to control the Agent during setup. WISH: to
 * refactor this. I removed what wasn't used and put a stub instead, just in case.
 */

public class LegacyAgentCommandsClientImpl implements AgentCommandsClient {
    private AgentConnection agentConn;
    private AgentCommandsAPI verAPI;

    /**
     * Create the object which communicates over a passed connection.
     * 
     * @args agentConn the connection to use when making requests to the agent.
     */
    public LegacyAgentCommandsClientImpl(AgentConnection agentConn) {
        this.agentConn = agentConn;
        this.verAPI = new AgentCommandsAPI();
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping()
        throws AgentRemoteException, AgentConnectionException {
        AgentPing_args args = new AgentPing_args();

        long sendTime = System.currentTimeMillis();
        this.agentConn.sendCommand(AgentCommandsAPI.command_ping,
                    this.verAPI.getVersion(), args, false);
        long recvTime = System.currentTimeMillis();

        return recvTime - sendTime;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#restart()
     */
    public void restart()
        throws AgentRemoteException, AgentConnectionException
    {
        AgentRestart_args args = new AgentRestart_args();

        this.agentConn.sendCommand(AgentCommandsAPI.command_restart,
                    this.verAPI.getVersion(), args);
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#die()
     */
    public void die()
        throws AgentRemoteException, AgentConnectionException
    {
        AgentDie_args args = new AgentDie_args();

        this.agentConn.sendCommand(AgentCommandsAPI.command_die,
                    this.verAPI.getVersion(), args);
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#getCurrentAgentBundle()
     */
    public String getCurrentAgentBundle()
        throws AgentRemoteException, AgentConnectionException {

        AgentBundle_args args = new AgentBundle_args();

        AgentRemoteValue cmdRes =
                    this.agentConn.sendCommand(AgentCommandsAPI.command_getCurrentAgentBundle,
                                this.verAPI.getVersion(),
                                args);

        return new AgentBundle_result(cmdRes).getCurrentAgentBundle();
    }

    public AgentUpdateFiles_result agentUpdateFiles(FileMetadata[] filesToUpdate,
                                                    String[] filesToRemove,
                                                    Boolean restartIfSuccessful)
        throws AgentRemoteException, AgentConnectionException {
        throw new IllegalAccessError(
                    "Unidirectional agentUpdateFiles method was invoked on a bi-directional commands client.");

    }

    public Map upgrade(String tarFile,
                       String destination)
        throws AgentRemoteException, AgentConnectionException {
        throw new IllegalAccessError(
                    "Unidirectional upgrade method was invoked on a bi-directional commands client.");
    }

    public Map<String, Boolean> agentRemoveFile(Collection<String> files)
        throws AgentRemoteException, AgentConnectionException {
        throw new IllegalAccessError(
                    "Unidirectional agentRemoveFile method was invoked on a bi-directional commands client.");
    }

}
