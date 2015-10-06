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

package org.hyperic.hq.util.properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertiesUtil {

    private static final Log logger = LogFactory.getLog(PropertiesUtil.class);
    private static int MAX_PORT_NUMBER = 65535;

    public static Boolean getBooleanValue(String property,
                                          boolean defaultValue) {
        if (StringUtils.isBlank(property)) {
            return defaultValue;
        }
        if (property.equalsIgnoreCase("true") || property.equalsIgnoreCase("yes")) {
            return true;
        }
        if (property.equalsIgnoreCase("false") || property.equalsIgnoreCase("no")) {
            return false;
        }
        return defaultValue;
    }

    public static int getIntValue(String propertyName,
                                  String propertyValueStr) {
        if (StringUtils.isBlank(propertyValueStr)) {
            throw new IllegalArgumentException("Empty property: " + propertyName);
        }
        return Integer.parseInt(propertyValueStr);
    }

    public static int getIntValue(String propertyName,
                                  String propertyValueStr,
                                  int defaultValue) {
        try {
            return getIntValue(propertyName, propertyValueStr);
        } catch (Exception exc) {
            // do nothing - keep default
        }
        return defaultValue;
    }

    public static int getGreatOrEqualZeroIntValue(String propertyName,
                                                  String propertyValueStr,
                                                  int defaultValue) {
        int res = getIntValue(propertyName, propertyValueStr, defaultValue);
        if (res < 0) {
            throw new IllegalArgumentException("Invalid value specified for the " + propertyName
                        + " property. The value must be equal to or greater than zero.");
        }
        return res;
    }

    public static long getLongValue(String propertyName,
                                    String propertyValueStr) {
        if (StringUtils.isBlank(propertyValueStr)) {
            throw new IllegalArgumentException("Empty property: " + propertyName);
        }
        try {
            return Long.parseLong(propertyValueStr);
        } catch (NumberFormatException e) {
            String msg = propertyName + " value is not a number: '" + propertyValueStr + "'";
            throw new IllegalArgumentException(msg);
        }
    }

    public static long getLongValue(String propertyName,
                                    String propertyValueStr,
                                    long defaultValue) {
        try {
            return getLongValue(propertyName, propertyValueStr);
        } catch (Exception exc) {
            String errMsg = String.format("%s. Setting default value of %s.", exc.getMessage(), defaultValue);
            logger.error(errMsg, exc);
        }
        return defaultValue;
    }

    /**
     * Validates that a port is within the range of [1,65535]
     * 
     * @param port
     * @throws IllegalArgumentException if it isn't
     */
    public static void validatePort(int port)
        throws IllegalArgumentException {
        if (port < 1 || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Port is out of range [1," + MAX_PORT_NUMBER + "]");
        }
    }

    public static int getPortValue(String propertyName,
                                   String propertyValueStr) {
        if (StringUtils.isBlank(propertyValueStr)) {
            throw new IllegalArgumentException("Empty property: " + propertyName);
        }

        int resPort = Integer.parseInt(propertyValueStr);
        validatePort(resPort);
        return resPort;

    }

    public static int getPortValue(String propertyName,
                                   String propertyValueStr,
                                   int defaultValue) {

        if ("*default*".equals(propertyValueStr)) {
            return defaultValue;
        }
        try {
            return getPortValue(propertyName, propertyValueStr);
        } catch (Exception exc) {
            // do nothing - keep default
        }
        return defaultValue;
    }

}
