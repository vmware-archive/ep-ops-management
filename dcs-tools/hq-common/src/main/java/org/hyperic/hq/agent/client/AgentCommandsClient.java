/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.agent.client;

import java.util.Map;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileMetadata;
import org.hyperic.hq.agent.commands.AgentUpdateFiles_result;

/**
 * Agent commands business logic.These are the methods that are invoked directly by the commands from the mailbox.
 * Beware of changing the API, without changing the server side as well
 * 
 */
public interface AgentCommandsClient {

    /**
     * Send a ping to the agent. This routine records the time the request is sent, and returns the round-trip time to
     * the caller.
     * 
     * @return the time it took (in milliseconds) for the round-trip time of the request to the agent.
     * 
     * @throws AgentRemoteException indicating the server failed to understand our request.
     * @throws AgentConnectionException indicating an error connecting to or communicating with the agent.
     */

    long ping()
        throws AgentRemoteException, AgentConnectionException;

    /**
     * Tell the agent to restart.
     * 
     * @throws AgentRemoteException indicating the server failed to understand our request.
     * @throws AgentConnectionException indicating an error connecting to or communicating with the agent.
     */

    void restart()
        throws AgentRemoteException, AgentConnectionException;

    /**
     * Tell the agent to die.
     * 
     * @throws AgentRemoteException indicating the server failed to understand our request.
     * @throws AgentConnectionException indicating an error connecting to or communicating with the agent.
     */

    void die()
        throws AgentRemoteException, AgentConnectionException;

    /**
     * Return the bundle that the agent is currently running.
     * 
     * @return The agent bundle name.
     * @throws AgentRemoteException indicating the server failed to understand our request.
     * @throws AgentConnectionException indicating an error connecting to or communicating with the agent.
     */
    String getCurrentAgentBundle()
        throws AgentRemoteException, AgentConnectionException;

    /**
     * Tell the agent to upgrade itself upon JVM restart.
     * 
     * @param tarFile Agent bundle tarball used to update the agent.
     * @param destination Destination directory on the agent where the bundle will reside.
     * 
     * @throws AgentRemoteException indicating the server failed to understand our request.
     * @throws AgentConnectionException indicating an error connecting to or communicating with the agent.
     */

    Map upgrade(String tarFile,
                String destination)
        throws AgentRemoteException, AgentConnectionException;

    /**
     * Update (remove, add new or overwrite) files on agent.
     * 
     * @param filesToUpdate
     * @param filesToRemove
     * @param restartIfSuccessful
     * @return AgentUpdateFiles_result
     * @throws AgentRemoteException
     * @throws AgentConnectionException
     */
    public AgentUpdateFiles_result agentUpdateFiles(FileMetadata[] filesToUpdate,
                                                    String[] filesToRemove,
                                                    Boolean restartIfSuccessful)
        throws AgentRemoteException, AgentConnectionException;
}
