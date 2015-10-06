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

package com.vmware.epops.util.converter;

import java.util.ArrayList;
import java.util.List;

import com.vmware.epops.model.config.NetworkConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class NetworkConfigConverter {

    private final static Logger logger = LoggerFactory.getLogger(NetworkConfigConverter.class.getName());

    private final static String FIELD_DELIMITER = ","; // [ip , netmask , mac]
    private final static String LIST_DELIMITER = ";"; // [ip , netmask , mac];[ip , netmask , mac]

    /**
     * Parse a string representation of the NetworkConfig Object - Format - [ip , netmask , mac].
     * 
     * @param objectStr
     * @return The {@link NetworkConfig} Object that the input objectStr String was represent.
     */
    public static NetworkConfig convertToNetworkConfig(String objectStr) {
        try {
            String networkConfigStrList = objectStr.subSequence(1, objectStr.length() - 1).toString();
            String[] networkConfigFields = networkConfigStrList.split(FIELD_DELIMITER);
            return new NetworkConfig(networkConfigFields[0].trim(), networkConfigFields[1].trim(),
                        networkConfigFields[2].trim());
        } catch (Exception e) {
            logger.error("Unable to convert {} to NetworkConfig Object", objectStr, e);
            return null;
        }
    }

    /**
     * Parse a string representation of the NetworkConfig Object, Format - [ip , netmask , mac];[ip , netmask , mac];[ip
     * , netmask , mac].
     * 
     * @param objectsStr
     * @return A List of {@link NetworkConfig} Object that the input objectsStr String was represent.
     */
    public static List<NetworkConfig> convertToNetworkConfigList(String objectsStr) {
        List<NetworkConfig> networkConfigs = new ArrayList<>();
        try {
            Iterable<String> networkConfigStrList =
                        Splitter.on(LIST_DELIMITER).trimResults().split(objectsStr);
            for (String networkConfigStr : networkConfigStrList) {
                NetworkConfig networkConfig = convertToNetworkConfig(networkConfigStr);
                if (networkConfig != null) {
                    networkConfigs.add(networkConfig);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to convert {} to list of NetworkConfig Objects", objectsStr, e);
        }
        return networkConfigs;
    }

    /**
     * Format a list of {@link NetworkConfig} Objects to their represents string in Format - [ip , netmask , mac];[ip ,
     * netmask , mac];[ip , netmask , mac].
     * 
     * @param networkConfigs
     * @return The represents string
     */
    public static String formatList(List<NetworkConfig> networkConfigs) {
        return Joiner.on(LIST_DELIMITER).join(networkConfigs);
    }

}
