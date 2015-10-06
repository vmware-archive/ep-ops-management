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

package com.vmware.epops.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.epops.command.upstream.measurement.MetricVal;

/**
 * Describes a resource for agent adapter command
 * 
 * @author Liat
 */
public class RawResource {

    private String resourceName;
    private String resourceType;
    private String rawIdentifier;
    private final Integer internalId;
    private String agentToken;
    private boolean sync; // true means that resource was reported as part of a "Sync Report"
    private List<RawResource> children;
    private Map<String, String> configProperties;
    private Map<String, String> discoveredProperties;
    private final Map<Integer, List<MetricVal>> metrics;

    public RawResource(String resourceName,
                       String resourceIdentifier,
                       String resourceType,
                       Integer internalId) {
        this(resourceName, resourceIdentifier, resourceType, internalId, null);
    }

    public RawResource(Integer internalId,
                       Map<Integer, List<MetricVal>> metrics) {
        this(null, null, null, internalId, metrics);
    }

    private RawResource(String resourceName,
                        String resourceIdentifier,
                        String resourceType,
                        Integer internalId,
                        Map<Integer, List<MetricVal>> metrics) {
        this.resourceName = resourceName;
        this.rawIdentifier = resourceIdentifier;
        this.resourceType = resourceType;
        this.internalId = internalId;
        this.metrics = metrics;
    }

    public String getRawIdentifier() {
        return rawIdentifier;
    }

    public void setRawIdentifier(String rawIdentifier) {
        this.rawIdentifier = rawIdentifier;
    }

    public Integer getInternalId() {
        return internalId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public List<RawResource> getChildren() {
        return ((children != null) ? children : Collections.<RawResource> emptyList());
    }

    public void setChildren(List<RawResource> children) {
        this.children = children;
    }

    public Map<String, String> getConfigProperties() {
        return ((configProperties != null) ? configProperties : new HashMap<String, String>());
    }

    public void setConfigProperties(Map<String, String> configProperties) {
        this.configProperties = configProperties;
    }

    public Map<String, String> getDiscoveredProperties() {
        return ((discoveredProperties != null) ? discoveredProperties : new HashMap<String, String>());
    }

    public void setDiscoveredProperties(Map<String, String> discoveredProperties) {
        this.discoveredProperties = discoveredProperties;
    }

    public Map<Integer, List<MetricVal>> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                    .append("internalId", internalId)
                    .toString();
    }
}
