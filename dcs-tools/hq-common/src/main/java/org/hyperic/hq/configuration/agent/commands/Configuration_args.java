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

package org.hyperic.hq.configuration.agent.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.security.SecurityUtil;
import org.springframework.util.CollectionUtils;

/**
 * Configuration arguments is being passed for configuration and scheduling purposes. It holds a set of resources
 * (actually one and its LWOs), a configuration for each, and a list of enabled metric names, among other things.
 */
public class Configuration_args extends AgentRemoteValue {

    /**
     * Key prefix constants. These constants are used in order to flatten configuration and scheduling data.
     */
    public static final String SCHEDULING_SEPERATOR = ".";
    public static final String RESOURCE_KIND_KEY = "id_resourceKind";
    public static final String RESOURCE_INTERNAL_ID_KEY = "id_resourceInternalId";
    public static final String MONITORED_RESOUCE_ID_KEY = "id_monitoresResourceId";
    public static final String PARENT_ID_KEY = "id_parentId";
    public static final String CONFIGUARTION_KEY_PREFIX = "config";
    public static final String SECURED_CONFIGUARTION_KEY_PREFIX = "secured.config";
    public static final String PROPERTIES_KEY_PREFIX = "prop";
    public static final String SCHEDULE_KEY_PREFIX = "schedule";
    public static final String IS_DELETE_RESOURCE = "isDelete";

    private int schedulingCounter = 0;

    public void setResourceKind(String resouceKind) {
        super.setValue(RESOURCE_KIND_KEY, resouceKind);
    }

    public String getResourceKind() {
        return getValue(RESOURCE_KIND_KEY);
    }

    public void setResourceInternalId(int internalId) {
        super.setValue(RESOURCE_INTERNAL_ID_KEY, Integer.toString(internalId));
    }

    public String getResourceInternalId() {
        return getValue(RESOURCE_INTERNAL_ID_KEY);
    }

    public void setParentId(String parentId) {
        super.setValue(PARENT_ID_KEY, parentId);
    }

    public String getParentId() {
        return getValue(PARENT_ID_KEY);
    }

    public void setMonitoredResourceId(String monitoredResourceId) {
        super.setValue(MONITORED_RESOUCE_ID_KEY, monitoredResourceId);
    }

    public String getMonitoredResourceId() {
        return getValue(MONITORED_RESOUCE_ID_KEY);
    }

    public void setConfiguration(Map<String, String> configuration) {
        addEntriesWithKeyPrefix(CONFIGUARTION_KEY_PREFIX, configuration);
    }

    public Map<String, String> getConfiguration() {
        return getEntriesWithoutKeyPrefix(CONFIGUARTION_KEY_PREFIX);
    }

    public void setSecuredConfiguration(Map<String, String> securedConfiguration) {
        addEntriesWithKeyPrefix(SECURED_CONFIGUARTION_KEY_PREFIX, securedConfiguration);
    }

    public Map<String, String> getSecuredConfiguration() {
        return getEntriesWithoutKeyPrefix(SECURED_CONFIGUARTION_KEY_PREFIX);
    }

    public void setProperties(Map<String, String> configuration) {
        addEntriesWithKeyPrefix(PROPERTIES_KEY_PREFIX, configuration);
    }

    public Map<String, String> getProperties() {
        return getEntriesWithoutKeyPrefix(PROPERTIES_KEY_PREFIX);
    }

    public void addScheduling(String metricName,
                              String metricId,
                              String pollingInterval) {
        StringBuilder value = new StringBuilder();
        value.append(metricId).append(SCHEDULING_SEPERATOR);
        value.append(pollingInterval).append(SCHEDULING_SEPERATOR);
        value.append(metricName);
        super.setValue(SCHEDULE_KEY_PREFIX + schedulingCounter, value.toString());
        schedulingCounter++;
    }

    /**
     * Returns a list of all scheduling. The list is comprised from String triplets: [0] - Metric Id [1] - Polling
     * Interval [2] - Metric Name
     * 
     * @return List of scheduling triplets
     */
    public List<String[]> getSchedulings() {
        Map<String, String> schedulings = getScheduleEntries();
        if (CollectionUtils.isEmpty(schedulings)) {
            return Collections.emptyList();
        }
        List<String[]> parsedSchedulings = new ArrayList<String[]>(schedulings.size());
        for (String schedule : schedulings.values()) {
            parsedSchedulings.add(schedule.split("\\" + SCHEDULING_SEPERATOR, 3));
        }
        return parsedSchedulings;
    }

    private Map<String, String> getScheduleEntries() {
        return getEntriesWithoutKeyPrefix(SCHEDULE_KEY_PREFIX);
    }

    private void addEntriesWithKeyPrefix(String prefix,
                                         Map<String, String> entries) {
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }
        for (Entry<String, String> entry : entries.entrySet()) {
            super.setValue(prefix + entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String> getEntriesWithoutKeyPrefix(String prefix) {
        Map<String, String> entries = getPrefixedValues(prefix);
        if (CollectionUtils.isEmpty(entries)) {
            return Collections.emptyMap();
        }
        Map<String, String> withoutPrefix = new HashMap<String, String>(entries.size());
        for (Entry<String, String> entry : entries.entrySet()) {
            withoutPrefix.put(entry.getKey().substring(prefix.length()), entry.getValue());
        }
        return withoutPrefix;
    }

    private Map<String, String> getEntriesWithoutKeyPrefix() {
        Map<String, String> prefixlessEntries = getConfiguration();
        prefixlessEntries.putAll(getSecuredConfiguration());
        prefixlessEntries.putAll(getProperties());
        prefixlessEntries.putAll(getScheduleEntries());

        return prefixlessEntries;
    }

    @Override
    public void setValue(String key,
                         String val) {
        throw new AgentAssertionException("This should never be called");
    }

    public void setIsDeleteResource(boolean deleteResource) {
        super.setValue(IS_DELETE_RESOURCE, String.valueOf(deleteResource));
    }

    public boolean isDeletedResource() {
        String val = super.getValue(IS_DELETE_RESOURCE);
        if (val != null) {
            return Boolean.valueOf(val);
        }
        return false;
    }

    @Override
    public String toString() {
        Map<String, String> safeVals = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : getEntriesWithoutKeyPrefix().entrySet()) {
            if (ConfigSchema.isSecret(entry.getKey()) || ConfigSchema.isSensitive(entry.getKey())) {
                safeVals.put(entry.getKey(), SecurityUtil.HIDDEN_VALUE_MASK);
            } else {
                safeVals.put(entry.getKey(), entry.getValue());
            }
        }

        return safeVals.toString();
    }
}
