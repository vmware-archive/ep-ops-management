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

package org.hyperic.hq.configuration.agent.repository;

import org.hyperic.hq.agent.AgentRemoteException;

public class MissingConfigurationInfoExeption extends AgentRemoteException {
    private static final long serialVersionUID = 1L;

    public enum MissingConfigurationInfoError {

        NULL_ARGS("Configuration command arguments are null"),
        MISSING_RESOUCE_KIND("Configuration info is missing resource kind property"),
        MISSING_RESOUCE_KEY("Configuration info is missing resource key elements"),
        MISSING_RESOUCE_INTERNAL_ID("Configuration info is missing resource internal id property"),
        BAD_RESOUCE_INTERNAL_ID("Configuration info resource internal id is not a valid number"),
        RESOURCE_KIND_HAS_NO_TYPE_INFO("Configuration info resource kind is unknown");

        private final String msg;

        private MissingConfigurationInfoError(String msg) {
            this.msg = msg;
        }

        public String getErrorMessage() {
            return msg;
        }

    };

    public MissingConfigurationInfoExeption(MissingConfigurationInfoError error) {
        super(error.getErrorMessage());
    }

    public MissingConfigurationInfoExeption(MissingConfigurationInfoError error,
                                            Exception ex) {
        super(error.getErrorMessage(), ex);
    }

    public MissingConfigurationInfoExeption(MissingConfigurationInfoError error,
                                            String problematicConfigurationValue) {
        super(error.getErrorMessage() + ": " + problematicConfigurationValue);
    }

}
