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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo;
import org.hyperic.hq.configuration.agent.repository.model.ResourceKey;
import org.hyperic.hq.configuration.agent.repository.model.SchedulingInfo;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.junit.Assert;
import org.junit.Test;

public class ResourceRepositoryServiceTest {

    private static final ConfigurationInfo MOCK_PLATFORM_CONFIG_INFO = createMockConfigInfo("", "1000", "Linux", 1,
                new PlatformTypeInfo());
    private static final ConfigurationInfo MOCK_SERVER_CONFIG_INFO = createMockConfigInfo("|1000", "c:\\tomcat",
                "Tomcat", 2, new ServerTypeInfo());
    private static final ConfigurationInfo MOCK_SERVER_2_CONFIG_INFO = createMockConfigInfo("|1000", "c:\\postgres",
                "Postgres", 3, new ServerTypeInfo());
    private static final ConfigurationInfo MOCK_SERVICE_CONFIG_INFO = createMockConfigInfo("|1000|c:\\tomcat",
                "webapp001", "Webapp", 4, new ServiceTypeInfo());
    private static final ConfigurationInfo MOCK_SERVICE_2_CONFIG_INFO = createMockConfigInfo("|1000|c:\\tomcat",
                "webapp002", "Webapp", 5, new ServiceTypeInfo());
    private static final ConfigurationInfo MOCK_SERVICE_3_CONFIG_INFO = createMockConfigInfo("|1000|c:\\postgres",
                "db001", "Database", 6, new ServiceTypeInfo());
    private static final ConfigurationInfo MOCK_SERVICE_4_CONFIG_INFO = createMockConfigInfo("|1000|c:\\postgres",
                "db002", "Database", 7, new ServiceTypeInfo());

    @Test
    public void getResourceAncestorsDepth1Repo3Test()
        throws ResourceRepositoryException {
        getResourceAncestorsDepth1Test(3);
    }

    @Test
    public void getResourceAncestorsDepth1Repo2Test()
        throws ResourceRepositoryException {
        getResourceAncestorsDepth1Test(2);
    }

    @Test
    public void getResourceAncestorsDepth1Repo1Test()
        throws ResourceRepositoryException {
        getResourceAncestorsDepth1Test(1);
    }

    @Test
    public void getResourceAncestorsDepth2Repo3Test()
        throws ResourceRepositoryException {
        getResourceAncestorsDepth2Test(3);
    }

    @Test
    public void getResourceAncestorsDepth2Repo2Test()
        throws ResourceRepositoryException {
        getResourceAncestorsDepth2Test(2);
    }

    @Test
    public void getResourceAncestorsDepth3Repo3Test()
        throws ResourceRepositoryException {
        getResourceAncestorsDepth3Test(3);
    }

    @Test
    public void getResourceDescendantsDepth1Repo3Test()
        throws ResourceRepositoryException {
        getResourceDescendantsDepth1Test(3);
    }

    @Test
    public void getResourceDescendantsDepth2Repo3Test()
        throws ResourceRepositoryException {
        getResourceDescendantsDepth2Test(3);
    }

    @Test
    public void getResourceDescendantsDepth2Repo2Test()
        throws ResourceRepositoryException {
        getResourceDescendantsDepth2Test(2);
    }

    @Test
    public void getResourceDescendantsDepth3Repo3Test()
        throws ResourceRepositoryException {
        getResourceDescendantsDepth3Test(3);
    }

    @Test
    public void getResourceDescendantsDepth3Repo2Test()
        throws ResourceRepositoryException {
        getResourceDescendantsDepth3Test(2);
    }

    @Test
    public void getResourceDescendantsDepth3Repo1Test()
        throws ResourceRepositoryException {
        getResourceDescendantsDepth3Test(1);
    }

    private void getResourceDescendantsDepth1Test(int repoDepthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = buildMockRepository(repoDepthLevel);

        List<ConfigurationInfo> descendants = repo.getResourceDescendents(MOCK_PLATFORM_CONFIG_INFO.getResourceKey());
        Assert.assertEquals(6, descendants.size());
        Assert.assertTrue(containsResource(MOCK_SERVER_CONFIG_INFO.getResourceKey(), descendants));
        Assert.assertTrue(containsResource(MOCK_SERVER_2_CONFIG_INFO.getResourceKey(), descendants));
        Assert.assertTrue(containsResource(MOCK_SERVICE_CONFIG_INFO.getResourceKey(), descendants));
        Assert.assertTrue(containsResource(MOCK_SERVICE_2_CONFIG_INFO.getResourceKey(), descendants));
        Assert.assertTrue(containsResource(MOCK_SERVICE_3_CONFIG_INFO.getResourceKey(), descendants));
        Assert.assertTrue(containsResource(MOCK_SERVICE_4_CONFIG_INFO.getResourceKey(), descendants));
    }

    private void getResourceDescendantsDepth2Test(int repoDepthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = buildMockRepository(repoDepthLevel);

        List<ConfigurationInfo> descendants = repo.getResourceDescendents(MOCK_SERVER_CONFIG_INFO.getResourceKey());
        Assert.assertEquals(2, descendants.size());
        Assert.assertTrue(containsResource(MOCK_SERVICE_CONFIG_INFO.getResourceKey(), descendants));
        Assert.assertTrue(containsResource(MOCK_SERVICE_2_CONFIG_INFO.getResourceKey(), descendants));
    }

    private void getResourceDescendantsDepth3Test(int repoDepthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = buildMockRepository(repoDepthLevel);

        List<ConfigurationInfo> descendants = repo.getResourceDescendents(MOCK_SERVICE_CONFIG_INFO.getResourceKey());
        Assert.assertTrue(descendants.isEmpty());
    }

    private void getResourceAncestorsDepth1Test(int repoDepthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = buildMockRepository(repoDepthLevel);

        List<ConfigurationInfo> hierarchy = repo.getResourceAncestors(MOCK_PLATFORM_CONFIG_INFO.getResourceKey());

        Assert.assertTrue(containsResource(MOCK_PLATFORM_CONFIG_INFO.getResourceKey(), hierarchy));
        Assert.assertEquals(1, hierarchy.size());
    }

    private void getResourceAncestorsDepth2Test(int repoDepthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = buildMockRepository(repoDepthLevel);

        List<ConfigurationInfo> hierarchy = repo.getResourceAncestors(MOCK_SERVER_CONFIG_INFO.getResourceKey());

        Assert.assertTrue(containsResource(MOCK_PLATFORM_CONFIG_INFO.getResourceKey(), hierarchy));
        Assert.assertTrue(containsResource(MOCK_SERVER_CONFIG_INFO.getResourceKey(), hierarchy));
        Assert.assertEquals(2, hierarchy.size());
    }

    private void getResourceAncestorsDepth3Test(int repoDepthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = buildMockRepository(repoDepthLevel);

        List<ConfigurationInfo> hierarchy = repo.getResourceAncestors(MOCK_SERVICE_CONFIG_INFO.getResourceKey());

        Assert.assertTrue(containsResource(MOCK_PLATFORM_CONFIG_INFO.getResourceKey(), hierarchy));
        Assert.assertTrue(containsResource(MOCK_SERVER_CONFIG_INFO.getResourceKey(), hierarchy));
        Assert.assertTrue(containsResource(MOCK_SERVICE_CONFIG_INFO.getResourceKey(), hierarchy));
        Assert.assertEquals(3, hierarchy.size());
    }

    private ResourceRepositoryService buildMockRepository(int depthLevel)
        throws ResourceRepositoryException {
        ResourceRepositoryService repo = ResourceRepositoryService.getInstance();
        if (depthLevel >= 1) {
            repo.compareAndSwap(MOCK_PLATFORM_CONFIG_INFO);
            if (depthLevel >= 2) {
                repo.compareAndSwap(MOCK_SERVER_CONFIG_INFO);
                repo.compareAndSwap(MOCK_SERVER_2_CONFIG_INFO);
                if (depthLevel >= 3) {
                    repo.compareAndSwap(MOCK_SERVICE_CONFIG_INFO);
                    repo.compareAndSwap(MOCK_SERVICE_2_CONFIG_INFO);
                    repo.compareAndSwap(MOCK_SERVICE_3_CONFIG_INFO);
                    repo.compareAndSwap(MOCK_SERVICE_4_CONFIG_INFO);
                }
            }
        }
        return repo;
    }

    private boolean containsResource(ResourceKey resourceKey,
                                     List<ConfigurationInfo> configs) {
        for (ConfigurationInfo config : configs) {
            if (config.getResourceKey().equals(resourceKey)) {
                return true;
            }
        }
        return false;
    }

    private static ConfigurationInfo createMockConfigInfo(String parentId,
                                                          String monitoredResourceId,
                                                          String resourceKind,
                                                          int resourceInternalId,
                                                          TypeInfo typeInfo) {

        ConfigurationInfo mock = new ConfigurationInfo();
        mock.setResourceKey(new ResourceKey(parentId, monitoredResourceId));
        mock.setResourceKind(resourceKind);
        mock.setTypeInfo(typeInfo);
        mock.setResourceInternalId(resourceInternalId);
        mock.setConfiguration(createMockConfiguration(100, resourceKind + "Config"));
        mock.setSecuredConfiguration(createMockConfiguration(100, resourceKind + "SecuredConfig"));
        mock.setScheduling(Collections.<SchedulingInfo> emptyList());
        return mock;

    }

    private static Map<String, String> createMockConfiguration(int size,
                                                               String keyPrefix) {
        Map<String, String> mock = new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            mock.put(keyPrefix + "_" + i, "Value_" + i);
        }
        return mock;
    }

}
