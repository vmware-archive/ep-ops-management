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

package com.vmware.epops.command.upstream.inventory;

import java.util.List;

import com.vmware.epops.model.RawResource;

/**
 * @since 20 Mar 2014
 * @author serebrob
 */
public class AiSendRuntimeReportCommandData extends ResourceHandlingAgentVerifiedCommandDataImpl {

    public static final String COMMAND_NAME = "aiSendRuntimeReport";

    public AiSendRuntimeReportCommandData(String agentToken,
                                          List<RawResource> resources) {
        super(agentToken, resources);
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

}
