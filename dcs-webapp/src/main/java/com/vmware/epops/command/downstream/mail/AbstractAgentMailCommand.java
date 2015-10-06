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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

abstract public class AbstractAgentMailCommand implements AgentMailCommand {

    private String commandDetails;

    @Override
    public String toString() {
        final ToStringBuilder toStringBuilder = new ToStringBuilder(this,
                    ToStringStyle.SHORT_PREFIX_STYLE);
        return toStringBuilder.toString();
    }

    @Override
    public String getCommandDetails() {
        return commandDetails;
    }

    @Override
    public void setCommandDetails(String commandDetails) {
        this.commandDetails = commandDetails;
    }

}
