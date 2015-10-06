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

public class ResourceRepositoryException extends Exception {

    enum ResourceRepositoryError {

        NULL_CONFIG("Configuration info is null"),
        MISSING_RESOURCE_KEY("Configuration info is missing a resource key"),
        MISSING_RESOURCE_KIND("Configuration info is missing a resource kind"),
        RESOURCE_KINDS_DIFFER(
                    "New Configuration resource kind differ. Cannot update."),
        MISSING_RESOURCE_TYPE_INFO("Configuration info is missing a resource type info"),
        MISSING_RESOURCE_PROPERTIES("Configuration info is missing must-have resource properties");

        private final String msg;

        private ResourceRepositoryError(String msg) {
            this.msg = msg;
        }

        public String getErrorMessage() {
            return msg;
        }

    };

    public ResourceRepositoryException(ResourceRepositoryError error) {
        super(error.msg);
    }

}
