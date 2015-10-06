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

package com.vmware.epops.plugin.converter;

import java.util.HashMap;
import java.util.Map;

import com.vmware.epops.plugin.Constants;

public class NameMappingUtil {

    private static final String REGEX_LEVEL_DELIMITER = "\\|";
    private static Map<String, String> resourceTypeNameMapping = new HashMap<>();
    private static Map<String, String> metricNameMapping = new HashMap<>();

    static {
        resourceTypeNameMapping.put("Win32", "Windows");
        resourceTypeNameMapping.put("Windows", "Win32");
        metricNameMapping.put("ResourceAvailability", "Availability");
        metricNameMapping.put("Availability", "Resource Availability");
    }

    private static String convertName(Map<String, String> namesMap,
                                      String name) {
        if (namesMap.containsKey(name)) {
            return namesMap.get(name);
        } else {
            return name;
        }
    }

    public static String convertResourceTypeName(String pluginResourceName) {
        return convertName(resourceTypeNameMapping, pluginResourceName);
    }

    public static Map<String, String> convertProperties(
                                                        Map<String, String> properties) {
        String typeProperty = properties.get(Constants.TYPE);
        if (typeProperty != null) {
            String convertedPropertyValue = convertResourceTypeName(typeProperty);
            properties.put(Constants.TYPE, convertedPropertyValue);
        }
        return properties;
    }

    public static String convertMetricName(String metricName) {
        String[] nodes = metricName.split(REGEX_LEVEL_DELIMITER);
        String name = nodes[nodes.length - 1];
        String convertedName = convertName(metricNameMapping, name);
        return metricName.replace(name, convertedName);
    }

}
