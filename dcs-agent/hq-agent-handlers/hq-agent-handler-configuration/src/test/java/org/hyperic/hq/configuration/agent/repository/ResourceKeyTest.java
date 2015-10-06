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

import junit.framework.Assert;

import org.hyperic.hq.configuration.agent.repository.model.ResourceKey;
import org.junit.Test;

public class ResourceKeyTest {

    public static final String PARENT_RESOUCE_MONITORED_ID = "1210";
    public static final String CHILD_RESOUCE_MONITORED_ID = "C:\\PATH";

    @Test
    public void testGetParentResourceKey() {
        ResourceKey resourceKey = new ResourceKey(
                    ResourceKey.ID_SEPARATOR + PARENT_RESOUCE_MONITORED_ID,
                    CHILD_RESOUCE_MONITORED_ID);
        ResourceKey parentResourceKey = new ResourceKey("", PARENT_RESOUCE_MONITORED_ID);
        Assert.assertEquals(parentResourceKey, resourceKey.getParentResourceKey());
    }

    @Test
    public void testGetNullParentResourceKey() {
        ResourceKey resourceKey = new ResourceKey("", PARENT_RESOUCE_MONITORED_ID);
        Assert.assertNull(resourceKey.getParentResourceKey());
    }

    @Test
    public void testGetIllegalParentResourceKey() {
        ResourceKey resourceKey = new ResourceKey("|", PARENT_RESOUCE_MONITORED_ID);
        Assert.assertNull(resourceKey.getParentResourceKey());
    }
}
