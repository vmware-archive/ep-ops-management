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

import org.junit.Assert;
import org.junit.Test;

import com.vmware.epops.plugin.Constants;
import com.vmware.epops.plugin.converter.NameMappingUtil;

public class NameMappingUtilTest {

    private static final String WINDOWS_NAME = "Windows";
    private static final String WIN32_NAME = "Win32";
    private static final String AVILABILTIY_NAME = "Availability";
    private static final String RECOURCE_AVIALABILITY = "Resource Availability";
    private static final String RECOURCE_AVIALABILITY_KEY = "ResourceAvailability";
    private static final String RECOURCE_KEY = "ResourceKind:lwo|";
    private static final String FULL_METRIC_KEY = RECOURCE_KEY + "ResourceAvailability";
    private static final String FULL_METRIC_KEY_CONVERTED = RECOURCE_KEY + "Availability";

    @Test
    public void convertResourceTypeNameTest() {
        Assert.assertEquals(WINDOWS_NAME,
                    NameMappingUtil.convertResourceTypeName(WIN32_NAME));
        Assert.assertEquals(WIN32_NAME,
                    NameMappingUtil.convertResourceTypeName(WINDOWS_NAME));
        Assert.assertEquals(Constants.OS_LINUX,
                    NameMappingUtil.convertResourceTypeName(Constants.OS_LINUX));
    }

    @Test
    public void convertPropertiesTest() {

        assetProperties(Constants.TYPE, WIN32_NAME, WINDOWS_NAME);
        assetProperties(Constants.TYPE, WINDOWS_NAME, WIN32_NAME);
        assetProperties(Constants.TYPE, Constants.OS_LINUX, Constants.OS_LINUX);
        assetProperties(Constants.FQDN, WIN32_NAME, WIN32_NAME);

    }

    private void assetProperties(String type,
                                 String name,
                                 String expected) {
        Map<String, String> properties = new HashMap<>();
        properties.put(type, name);
        NameMappingUtil.convertProperties(properties);
        Assert.assertEquals(expected, properties.get(type));

    }

    @Test
    public void convertMetricNameTest() {
        Assert.assertEquals(RECOURCE_AVIALABILITY,
                    NameMappingUtil.convertMetricName(AVILABILTIY_NAME));
        Assert.assertEquals(AVILABILTIY_NAME,
                    NameMappingUtil.convertMetricName(RECOURCE_AVIALABILITY_KEY));
        Assert.assertEquals(RECOURCE_AVIALABILITY,
                    NameMappingUtil.convertMetricName(RECOURCE_AVIALABILITY));
        Assert.assertEquals(FULL_METRIC_KEY_CONVERTED,
                    NameMappingUtil.convertMetricName(FULL_METRIC_KEY));
    }

}
