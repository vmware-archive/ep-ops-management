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

package org.hyperic.hq.configuration.agent.repository.model;

import org.apache.commons.lang.StringUtils;

public class ResourceKey {

    public static final String ID_SEPARATOR = "|";

    private String resourceMonitoredId;
    private String parentId;
    private ResourceKey parentResourceKey;

    public ResourceKey(String parentId,
                       String resourceMonitoredId) {
        this.parentId = parentId;
        this.resourceMonitoredId = resourceMonitoredId;
        this.parentResourceKey = calcParentResourceKey();
    }

    public String getResourceMonitoredId() {
        return resourceMonitoredId;
    }

    public String getParentId() {
        return parentId;
    }

    public ResourceKey getParentResourceKey() {
        return parentResourceKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ResourceKey)) {
            return false;
        }
        ResourceKey other = (ResourceKey) obj;
        boolean equals = resourceMonitoredId.equals(other.resourceMonitoredId);
        equals &= parentId.equals(other.parentId);
        return equals;
    }

    @Override
    public int hashCode() {
        return resourceMonitoredId.hashCode() + parentId.hashCode();
    }

    @Override
    public String toString() {
        return parentId + " " + resourceMonitoredId;
    }

    private ResourceKey calcParentResourceKey() {
        if (StringUtils.isEmpty(parentId)) {
            return null;
        }
        String parentParentId = StringUtils.substringBeforeLast(parentId, ID_SEPARATOR);
        if (StringUtils.isEmpty(parentParentId)) {
            parentParentId = "NO_PARENT";
        }
        String parentMonitoredResourceId = StringUtils.substringAfterLast(parentId, ID_SEPARATOR);
        if (StringUtils.isEmpty(parentMonitoredResourceId)) {
            return null;
        }
        return new ResourceKey(parentParentId, parentMonitoredResourceId);
    }
}
