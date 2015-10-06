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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.configuration.agent.commands.Configuration_args;
import org.hyperic.hq.configuration.agent.repository.MissingConfigurationInfoExeption.MissingConfigurationInfoError;
import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo;
import org.hyperic.hq.configuration.agent.repository.model.ResourceKey;
import org.hyperic.hq.configuration.agent.repository.model.SchedulingInfo;
import org.hyperic.hq.product.TypeInfo;
import org.springframework.util.CollectionUtils;

/**
 * Translates configuration_args (originated from the server) to ConfigInfo. The translation re-constructs the complex
 * structure of configuration and scheduling.
 */
public class ConfigurationInfoTranslator {

    private static final Log logger = LogFactory.getLog(ConfigurationInfoTranslator.class);
    private static Pattern LWO_CONFIG_PATTERN = Pattern.compile("(.+):(.+)\\|(.+)");
    private static final String PIPE_SEPERATOR = "\\|";
    private static final String VIRTUAL_SERVERS_PROP_NAME = "virtual_servers";
    private static final String VIRTUAL_SERVER_STRING_LIST_SEPERATOR = ";";
    private static final String VIRTUAL_SERVER_NAME_PROPERTIES_SEPERATOR = ":";
    private static final String VIRTUAL_SERVER_PROP_SEPERATOR = "=";
    private static final String VIRTUAL_SERVER_PROP_LIST_DELIMITERS = "{}";
    private static final String VIRTUAL_SERVER_PROP_LIST_SEPERATOR = ",";

    public enum PlatformKeyConfigPrefix {
        NAME("name"), FQDN("fqdn"), IP("ip"), TYPE("type"), ID("id");

        public static final String PREFIX = "platform.";

        private final String key;

        private PlatformKeyConfigPrefix(String key) {
            this.key = key;
        }

        public static String prefixKey(String key) {
            for (PlatformKeyConfigPrefix platformConfigKey : values()) {
                if (platformConfigKey.key.equals(key)) {
                    return PREFIX + key;
                }
            }
            return key;
        }

        public String getKey() {
            return key;
        }

        public String getPrefixedKey() {
            return PREFIX + key;
        }
    }

    public static ConfigurationInfo translate(Configuration_args configurationArgs,
                                              AgentDaemon agentDaemon)
        throws MissingConfigurationInfoExeption {

        if (configurationArgs == null) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.NULL_ARGS);
        }
        ConfigurationInfo configInfo = new ConfigurationInfo();
        translateResourceKey(configurationArgs, configInfo);
        translateResourceKind(configurationArgs, configInfo, agentDaemon);
        translateResourceInternalId(configurationArgs, configInfo);
        translateSchedulings(configurationArgs, configInfo);
        translateConfiguration(configurationArgs, configInfo);
        return configInfo;
    }

    private static void addTypeInfo(Configuration_args configurationArgs,
                                    AgentDaemon agentDaemon,
                                    ConfigurationInfo configInfo)
        throws MissingConfigurationInfoExeption {
        String resourceKind = configurationArgs.getResourceKind();
        try {
            TypeInfo typeInfo = agentDaemon.getTypeInfo(resourceKind);
            configInfo.setTypeInfo(typeInfo);
        } catch (Exception e) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.RESOURCE_KIND_HAS_NO_TYPE_INFO, e);
        }
        if (null == configInfo.getTypeInfo()) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.RESOURCE_KIND_HAS_NO_TYPE_INFO,
                        resourceKind);
        }
    }

    private static void translateResourceKind(Configuration_args configurationArgs,
                                              ConfigurationInfo configInfo,
                                              AgentDaemon agentDaemon)
        throws MissingConfigurationInfoExeption {
        String resourceKind = configurationArgs.getResourceKind();
        if (resourceKind == null) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.MISSING_RESOUCE_KIND);
        }
        configInfo.setResourceKind(resourceKind);
        addTypeInfo(configurationArgs, agentDaemon, configInfo);
    }

    public static void translateResourceInternalId(Configuration_args configurationArgs,
                                                   ConfigurationInfo configInfo)
        throws MissingConfigurationInfoExeption {
        String internalId = configurationArgs.getResourceInternalId();
        if (internalId == null) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.MISSING_RESOUCE_INTERNAL_ID);
        }
        try {
            configInfo.setResourceInternalId(Integer.parseInt(internalId));
        } catch (NumberFormatException e) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.BAD_RESOUCE_INTERNAL_ID);
        }
    }

    private static void translateResourceKey(Configuration_args configurationArgs,
                                             ConfigurationInfo configInfo)
        throws MissingConfigurationInfoExeption {
        String monitoresResourceId = configurationArgs.getMonitoredResourceId();
        String parentId = configurationArgs.getParentId();
        if (monitoresResourceId == null || parentId == null) {
            throw new MissingConfigurationInfoExeption(MissingConfigurationInfoError.MISSING_RESOUCE_KEY);
        }
        configInfo.setResourceKey(new ResourceKey(parentId, monitoresResourceId));
    }

    private static void translateSchedulings(Configuration_args configurationArgs,
                                             ConfigurationInfo configInfo) {
        List<String[]> rawSchedulings = configurationArgs.getSchedulings();
        if (CollectionUtils.isEmpty(rawSchedulings)) {
            configInfo.setScheduling(Collections.<SchedulingInfo> emptyList());
        }
        List<SchedulingInfo> schedulings = new ArrayList<SchedulingInfo>(rawSchedulings.size());
        Map<String, List<SchedulingInfo>> lwoScheduling = new HashMap<String, List<SchedulingInfo>>();
        for (String[] rawSchedule : rawSchedulings) {
            try {

                String[] lwo = extractLwoTypeAndInstance2Key(rawSchedule[2]);
                if (lwo == null) {
                    schedulings.add(new SchedulingInfo(rawSchedule[2], Integer.parseInt(rawSchedule[0]),
                                Long.parseLong(rawSchedule[1])));
                } else {
                    SchedulingInfo schedulingInfo = new SchedulingInfo(lwo[1], Integer.parseInt(rawSchedule[0]),
                                Long.parseLong(rawSchedule[1]));
                    if (lwoScheduling.containsKey(lwo[0])) {
                        lwoScheduling.get(lwo[0]).add(schedulingInfo);
                    } else {
                        ArrayList<SchedulingInfo> lwoMetrics = new ArrayList<SchedulingInfo>();
                        lwoMetrics.add(schedulingInfo);
                        lwoScheduling.put(lwo[0], lwoMetrics);
                    }

                }
            } catch (NumberFormatException e) {
                logger.error("Failed to parse this schedule info: " + convertRawSchedulingToString(rawSchedule));
                continue;
            }
        }
        configInfo.setScheduling(schedulings);
        configInfo.setLwoScheduling(lwoScheduling);
    }

    private static void translateConfiguration(Configuration_args configurationArgs,
                                               ConfigurationInfo configInfo) {
        // 1. Add configuration
        configInfo.setConfiguration(configurationArgs.getConfiguration());
        // 2. Add secured configuration
        configInfo.setSecuredConfiguration(configurationArgs.getSecuredConfiguration());
        // 3. Add Properties
        // Split resource properties from LWO properties
        // (LWO properties are mixed with the resource properties)
        Map<String, String> allProperties = configurationArgs.getProperties();
        configInfo.setProperties(new HashMap<String, String>());
        configInfo.setLwoConfiguration(new HashMap<String, Map<String, String>>());
        for (Entry<String, String> configEntry : allProperties.entrySet()) {
            /**
             * The allProperties map consists of two types of properties: a resource property and LWO property. A LWO
             * property is of the form <type>:<instanceName>|configKey. Thus, we first try to determine if a given key
             * of allProperties is LWO.
             */
            String[] lwoTypeAndInstance2Key = extractLwoTypeAndInstance2Key(configEntry.getKey());
            // If null, key is a resource property
            if (lwoTypeAndInstance2Key == null) {
                String key = prefixPlatformConfigKey(configEntry.getKey(), configInfo.getTypeInfo());
                if (key.equals(VIRTUAL_SERVERS_PROP_NAME)) {
                    configInfo.setVirtualServerConfiguration(getVirtualServers(configEntry.getValue()));
                } else {
                    configInfo.getProperties().put(key, configEntry.getValue());
                }
            } else { // else, its a LWO
                Map<String, String> lwoConfiguration = configInfo.getLwoConfiguration().get(lwoTypeAndInstance2Key[0]);
                if (lwoConfiguration == null) {
                    lwoConfiguration = new HashMap<String, String>();
                }
                lwoConfiguration.put(lwoTypeAndInstance2Key[1], configEntry.getValue());
                configInfo.getLwoConfiguration().put(lwoTypeAndInstance2Key[0], lwoConfiguration);
            }
        }
    }

    private static String prefixPlatformConfigKey(String key,
                                                  TypeInfo typeInfo) {
        if (TypeInfo.TYPE_PLATFORM == typeInfo.getType()) {
            return PlatformKeyConfigPrefix.prefixKey(key);
        }
        return key;
    }

    /**
     * Extract the virtual servers properties. These properties are transfer as one string on one of the platform
     * properties, with key name "virtual_servers". The value is a string formatted as follow: <vServer
     * type>:{<propKey1>=<value>, <propKey2>=<value>};<vServer type>:{<propKey1>=<value>, <propKey2>=<value>} i.e.:
     * virtual_servers=FileServer:{installpath=/, name=%serverName% FileServer};HyperVServer:{installpath=/,
     * name=%serverName% HyperVServer};
     * 
     * @param virtualServerStrList
     * @return
     */
    private static Map<String, Map<String, String>> getVirtualServers(String virtualServerStrList) {

        Map<String, Map<String, String>> virtualServerProperties = new HashMap<String, Map<String, String>>();

        for (String virtualServerStr : StringUtils.split(virtualServerStrList, VIRTUAL_SERVER_STRING_LIST_SEPERATOR)) {
            String[] fields = StringUtils.split(virtualServerStr, VIRTUAL_SERVER_NAME_PROPERTIES_SEPERATOR);
            if (fields.length != 2) {
                logger.error("unexpected properties structure for virtual server");
                continue;
            }

            String vServerName = fields[0];
            String virtualServerPropStrList = StringUtils.strip(fields[1], VIRTUAL_SERVER_PROP_LIST_DELIMITERS);
            Map<String, String> vServerProperties = new HashMap<String, String>();
            for (String virtualServerProp : StringUtils.split(virtualServerPropStrList,
                        VIRTUAL_SERVER_PROP_LIST_SEPERATOR)) {
                vServerProperties.put(StringUtils.substringBefore(virtualServerProp, VIRTUAL_SERVER_PROP_SEPERATOR),
                            StringUtils.substringAfter(virtualServerProp, VIRTUAL_SERVER_PROP_SEPERATOR));
            }

            virtualServerProperties.put(vServerName, vServerProperties);
        }
        return virtualServerProperties;
    }

    private static String[] extractLwoTypeAndInstance2Key(String key) {
        // If pattern matched, then this is LWO, else return null
        if (isLwoPattern(key)) {
            return key.split(PIPE_SEPERATOR, 2);
        }
        return null;
    }

    /**
     * Checks if a given key is of the pattern ResourceKind:InstanceName|MetricGroup|Metric Note that InstanceName might
     * contain a ":"
     * 
     * @param key
     */
    private static boolean isLwoPattern(String key) {
        Matcher m = LWO_CONFIG_PATTERN.matcher(key);
        return m.matches() && m.groupCount() == 3;
    }

    private static String convertRawSchedulingToString(String[] rawSchedule) {
        StringBuilder sb = new StringBuilder("Failed to parse this schdule info: ");
        sb.append("Name: ").append(rawSchedule[2]);
        sb.append("ID: ").append(rawSchedule[0]);
        sb.append("Polling interval: ").append(rawSchedule[1]);
        return sb.toString();
    }
}
