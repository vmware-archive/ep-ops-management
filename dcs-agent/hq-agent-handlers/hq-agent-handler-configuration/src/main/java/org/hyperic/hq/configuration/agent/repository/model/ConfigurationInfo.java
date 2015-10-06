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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hyperic.hq.product.TypeInfo;

public class ConfigurationInfo {

    private ResourceKey resourceKey;
    private String resourceKind;
    private int resourceInternalId;
    private Map<String, String> properties;
    private Map<String, String> configuration;
    private Map<String, String> securedConfiguration;
    private Map<String, Map<String, String>> lwoConfiguration;
    private Map<String, Map<String, String>> virtualServerConfiguration;
    private List<SchedulingInfo> scheduling;
    private Map<String, List<SchedulingInfo>> lwoScheduling;
    private TypeInfo typeInfo;

    public enum ChangeType {
        EQUAL, CONFIGURATION, SCHEDULING, BOTH;
    }

    public ResourceKey getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(ResourceKey resourceKey) {
        this.resourceKey = resourceKey;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public Map<String, String> getSecuredConfiguration() {
        return securedConfiguration;
    }

    public void setSecuredConfiguration(Map<String, String> securedconfiguration) {
        this.securedConfiguration = securedconfiguration;
    }

    public Map<String, Map<String, String>> getLwoConfiguration() {
        return lwoConfiguration;
    }

    public void setLwoConfiguration(Map<String, Map<String, String>> lwoConfiguration) {
        this.lwoConfiguration = lwoConfiguration;
    }

    public Map<String, Map<String, String>> getVirtualServerConfiguration() {
        return virtualServerConfiguration;
    }

    public void setVirtualServerConfiguration(Map<String, Map<String, String>> virtualServerConfiguration) {
        this.virtualServerConfiguration = virtualServerConfiguration;
    }

    public List<SchedulingInfo> getScheduling() {
        return scheduling;
    }

    public void setScheduling(List<SchedulingInfo> scheduling) {
        this.scheduling = scheduling;
    }

    public Map<String, List<SchedulingInfo>> getLwoScheduling() {
        return lwoScheduling;
    }

    public void setLwoScheduling(Map<String, List<SchedulingInfo>> lwoScheduling) {
        this.lwoScheduling = lwoScheduling;
    }

    public String getResourceKind() {
        return resourceKind;
    }

    public void setResourceKind(String resourceKind) {
        this.resourceKind = resourceKind;
    }

    public int getResourceInternalId() {
        return resourceInternalId;
    }

    public void setResourceInternalId(int resourceInternalId) {
        this.resourceInternalId = resourceInternalId;
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    private boolean isConfigurationEquals(ConfigurationInfo other) {
        return (!configuration.equals(other.configuration)) &&
                    (!securedConfiguration.equals(other.securedConfiguration)) &&
                    (!lwoConfiguration.equals(other.lwoConfiguration));
    }

    private boolean isSchedulingEquals(ConfigurationInfo other) {
        return scheduling.equals(other.scheduling);
    }

    public ChangeType compare(ConfigurationInfo other) {
        if (other == null) {
            return ChangeType.BOTH;
        }

        if (!resourceKey.equals(other.resourceKey) || !resourceKind.equals(other.resourceKind)) {
            return null;
        }

        if (isConfigurationEquals(other)) {
            if (isSchedulingEquals(other)) {
                return ChangeType.EQUAL;
            } else {
                return ChangeType.SCHEDULING;
            }
        } else {
            if (isSchedulingEquals(other)) {
                return ChangeType.CONFIGURATION;
            } else {
                return ChangeType.BOTH;
            }
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                    append("resourceInternalId", resourceInternalId).
                    append("resourceKind", resourceKind).
                    append("resourceKey", resourceKey).
                    append("properties", properties).
                    append("configuration", configuration).
                    append("lwoConfiguration", lwoConfiguration).
                    append("virtualServerConfiguration", virtualServerConfiguration).
                    append("scheduling", scheduling).
                    append("lwoScheduling", lwoScheduling).
                    append("typeInfo", typeInfo).
                    toString();
    }
}
