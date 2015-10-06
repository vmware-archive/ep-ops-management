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

package com.vmware.epops.command.downstream.mail.management;

import com.vmware.epops.command.downstream.mail.AgentMailCommandType;

public class UpgradeCommand extends AgentManagementCommand {

    public static final AgentMailCommandType COMMAND_TYPE = AgentMailCommandType.UPGRADE;
    private String tarFile;
    private String destination;

    public UpgradeCommand() {
    }

    /**
     * Tell the agent to upgrade itself upon JVM restart.
     * 
     * @param tarFile Agent bundle tarball used to update the agent.
     * @param destination Destination directory on the agent where the bundle will reside.
     */
    public UpgradeCommand(String tarFile,
                          String destination) {
        super();
        this.tarFile = tarFile;
        this.destination = destination;
    }

    public String getTarFile() {
        return tarFile;
    }

    public String getDestination() {
        return destination;
    }

    public void setTarFile(String tarFile) {
        this.tarFile = tarFile;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public AgentMailCommandType getCommandType() {
        return COMMAND_TYPE;
    }

}
