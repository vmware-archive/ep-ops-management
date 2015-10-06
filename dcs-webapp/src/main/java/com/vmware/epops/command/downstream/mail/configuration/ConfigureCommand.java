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

package com.vmware.epops.command.downstream.mail.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.vmware.epops.command.downstream.mail.AbstractAgentMailCommand;
import com.vmware.epops.command.downstream.mail.AgentMailCommandType;
import com.vmware.epops.cryptography.CryptographyService;

/**
 * Configure command is a single command sent from the server to the agent for configuration and scheduling purposes.
 * The command is per resource, and it will hold it's configuration and a list of the enabled metric keys/names along
 * with the metric's internalId, interval, etc.
 * 
 * @author rina
 */
public class ConfigureCommand extends AbstractAgentMailCommand {

    private Integer resourceId;
    private String resourceKind;
    /**
     * A {@link Map} of configuration key and value, representing public configuration data (i.e. identifiers,
     * config-properties, and none-secured properties)
     */
    private Map<String, String> publicConfiguration;

    /**
     * A {@link Map} of configuration key and value, representing secured data (i.e. passwords)
     */
    private Map<String, String> securedConfiguration;

    private Map<String, String> properties; // non-configurable data

    private List<MetricScheduling> enableMetrics; // this will include lwo metrics too

    private boolean isDeleteResource = false;

    public ConfigureCommand() {
        publicConfiguration = new HashMap<>();
        securedConfiguration = new HashMap<>();
        properties = new HashMap<>();
        enableMetrics = new ArrayList<>();
    }

    @Override
    public AgentMailCommandType getCommandType() {
        return AgentMailCommandType.CONFIGURE_RESOURCE;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceKind() {
        return resourceKind;
    }

    public void setResourceKind(String resourceKind) {
        this.resourceKind = resourceKind;
    }

    public void setPublicConfiguration(Map<String, String> publicConfiguration) {
        this.publicConfiguration = publicConfiguration;
    }

    public void setSecuredConfiguration(Map<String, String> securedConfiguration) {
        this.securedConfiguration = securedConfiguration;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setEnableMetrics(List<MetricScheduling> enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public boolean isDeleteResource() {
        return isDeleteResource;
    }

    public void setDeleteResource(boolean deleteResource) {
        this.isDeleteResource = deleteResource;
    }

    /**
     * @return the resouce configuration map. If there are sensitive entries that needs to be secured, they will appear
     *         encrypted
     */
    public Map<String, String> getConfiguration() {
        Map<String, String> resourceConfigurationMap = new HashMap<>();
        resourceConfigurationMap.putAll(publicConfiguration);
        resourceConfigurationMap.putAll(securedConfiguration);
        return resourceConfigurationMap;
    }

    public Map<String, String> getPublicConfiguration() {
        return publicConfiguration;
    }

    public Map<String, String> getSecuredConfiguration() {
        return securedConfiguration;
    }

    /**
     * Returns the secured resource's configuration map. Its secured entries decrypted.
     * 
     * @return resource's configuration map
     */
    public Map<String, String> getSecuredConfigurationDecrypted() {
        Map<String, String> resourceConfigurationMap = new HashMap<>();
        for (Entry<String, String> entry : securedConfiguration.entrySet()) {
            String key = entry.getKey();
            String value = CryptographyService.decrypt(entry.getValue());
            resourceConfigurationMap.put(key, value);
        }
        return resourceConfigurationMap;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean containsSecuredConfiguration() {
        return !securedConfiguration.isEmpty();
    }

    public List<MetricScheduling> getEnableMetrics() {
        return enableMetrics;
    }
}
