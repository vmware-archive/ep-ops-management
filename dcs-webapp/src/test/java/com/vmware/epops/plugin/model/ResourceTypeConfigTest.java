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

package com.vmware.epops.plugin.model;

import org.junit.Assert;
import org.junit.Test;

import com.vmware.epops.plugin.model.ResourceTypeConfig;

public class ResourceTypeConfigTest {

    @Test
    public void testHashCodeEqualsResourceTypeConfig()
        throws Exception {
        ResourceTypeConfig resourceTypeConfig1 =
                    new ResourceTypeConfig("name", "name", "desc", "type", true, "defaultValue", false, null);
        ResourceTypeConfig resourceTypeConfig2 =
                    new ResourceTypeConfig("name", "name", "desc", "type", true, "defaultValue", false, null);

        Assert.assertTrue("PluginResourceType objects are not equal", resourceTypeConfig1.equals(resourceTypeConfig2));
        Assert.assertTrue("PluginResourceType hashcodes are different",
                    resourceTypeConfig1.hashCode() == resourceTypeConfig2.hashCode());
        Assert.assertTrue("PluginResourceType toStrings are different",
                    resourceTypeConfig1.toString().equals(resourceTypeConfig2.toString()));
    }

    @Test
    public void testHashCodeNotEqualsResourceTypeConfig()
        throws Exception {

        ResourceTypeConfig resourceTypeConfig1 =
                    new ResourceTypeConfig("name111111111", "name111111111", "desc", "type", true, "defaultValue",
                                false, null);
        ResourceTypeConfig resourceTypeConfig2 =
                    new ResourceTypeConfig("name", "name", "desc", "type", true, "defaultValue", false, null);

        Assert.assertTrue("PluginResourceType objects are equal", !resourceTypeConfig1.equals(resourceTypeConfig2));
        Assert.assertTrue("PluginResourceType hashcodes are equal",
                    resourceTypeConfig1.hashCode() != resourceTypeConfig2.hashCode());
        Assert.assertTrue("PluginResourceType toStrings are equal",
                    !resourceTypeConfig1.toString().equals(resourceTypeConfig2.toString()));

    }
}
