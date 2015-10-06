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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.configuration.agent.commands.Configuration_args;
import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo;
import org.hyperic.hq.configuration.agent.repository.model.ResourceKey;
import org.hyperic.hq.configuration.agent.repository.model.SchedulingInfo;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This class tests if the ConfigurationInfo translator works properly. It tests each data stored in a
 * ConfigurationInfo.
 */
public class ConfigurationInfoTranslatorTest {

    private static final String RESOURCE_KIND = "resourceKind";
    private static final String PARENT_ID = "parentId";
    private static final String MONITORED_RESOURCE_ID = "monitoredResourceId";
    private static final int RESOURCE_INTERNAL_ID = 1001;
    private AgentDaemon agentDaemon;

    @Before
    public void setup() {
        agentDaemon = EasyMock.createMock(AgentDaemon.class);
    }

    /**
     * Tests If a ResourceKind, ResourceKey and ResourceInternalId are translated correctly.
     * 
     * @throws MissingConfigurationInfoExeption
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test
    public void translateResourceKindKeyAndIdTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        Configuration_args args = new Configuration_args();
        args.setResourceKind(RESOURCE_KIND);
        args.setParentId(PARENT_ID);
        args.setMonitoredResourceId(MONITORED_RESOURCE_ID);
        args.setResourceInternalId(RESOURCE_INTERNAL_ID);

        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfo configInfo = ConfigurationInfoTranslator.translate(args, agentDaemon);

        ResourceKey resourceKey = new ResourceKey(PARENT_ID, MONITORED_RESOURCE_ID);
        Assert.assertEquals(RESOURCE_KIND, configInfo.getResourceKind());
        Assert.assertEquals(resourceKey, configInfo.getResourceKey());
        Assert.assertEquals(RESOURCE_INTERNAL_ID, configInfo.getResourceInternalId());
        EasyMock.verify();
    }

    /**
     * Tests the case where ResouceKind is null.
     */
    @Test(expected = MissingConfigurationInfoExeption.class)
    public void translateResourceKindNullTest()
        throws MissingConfigurationInfoExeption {
        Configuration_args args = new Configuration_args();
        args.setParentId(PARENT_ID);
        args.setMonitoredResourceId(MONITORED_RESOURCE_ID);
        args.setResourceInternalId(RESOURCE_INTERNAL_ID);
        ConfigurationInfoTranslator.translate(args, agentDaemon);
    }

    /**
     * Tests the case where one of the ResouceKey properties is null.
     */
    @Test(expected = MissingConfigurationInfoExeption.class)
    public void translateResourceKeyNullTest()
        throws MissingConfigurationInfoExeption {
        Configuration_args args = new Configuration_args();
        args.setResourceInternalId(RESOURCE_INTERNAL_ID);
        args.setResourceKind(RESOURCE_KIND);
        ConfigurationInfoTranslator.translate(args, agentDaemon);
    }

    /**
     * Tests the case where ResouceKind is null.
     * 
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test(expected = MissingConfigurationInfoExeption.class)
    public void translateResourceInternalIdNullTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        Configuration_args args = new Configuration_args();
        args.setParentId(PARENT_ID);
        args.setMonitoredResourceId(MONITORED_RESOURCE_ID);
        args.setResourceKind(RESOURCE_KIND);
        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfoTranslator.translate(args, agentDaemon);
        EasyMock.verify();
    }

    /**
     * Tests whether scheduling are translated correctly.
     * 
     * @throws MissingConfigurationInfoExeption
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test
    public void translateSchedulingsTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        List<String[]> schedulings = createMockScheduling();
        List<String[]> lwoSchedulings = createMockLwoScheduling();
        Configuration_args args = createBaseConfigurationArgs();
        args = addSchedulings(args, schedulings);
        args = addSchedulings(args, lwoSchedulings);
        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfo configInfo = ConfigurationInfoTranslator.translate(args, agentDaemon);

        for (String[] schedule : schedulings) {
            SchedulingInfo schedulingInfo = new SchedulingInfo(schedule[2], Integer.parseInt(schedule[0]),
                        Long.parseLong(schedule[1]));
            Assert.assertTrue(configInfo.getScheduling().contains(schedulingInfo));
        }
        EasyMock.verify();
    }

    /**
     * Tests whether LWO scheduling are translated correctly.
     * 
     * @throws MissingConfigurationInfoExeption
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test
    public void translateLwoSchedulingsTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        List<String[]> schedulings = createMockScheduling();
        List<String[]> lwoSchedulings = createMockLwoScheduling();
        Configuration_args args = createBaseConfigurationArgs();
        args = addSchedulings(args, schedulings);
        args = addSchedulings(args, lwoSchedulings);
        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfo configInfo = ConfigurationInfoTranslator.translate(args, agentDaemon);

        for (String[] schedule : lwoSchedulings) {
            String lwo = StringUtils.substringBeforeLast(schedule[2], "|");
            String metricName = StringUtils.substringAfterLast(schedule[2], "|");
            SchedulingInfo schedulingInfo = new SchedulingInfo(metricName, Integer.parseInt(schedule[0]),
                        Long.parseLong(schedule[1]));
            Assert.assertTrue(configInfo.getLwoScheduling().get(lwo).contains(schedulingInfo));
        }
        EasyMock.verify();
    }

    /**
     * Tests whether secured configuration are translated correctly.
     * 
     * @throws MissingConfigurationInfoExeption
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test
    public void translateSecuredConfigurationTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        Map<String, String> configuration = createMockMap(100);
        Configuration_args args = createBaseConfigurationArgs();
        args.setSecuredConfiguration(configuration);
        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfo configInfo = ConfigurationInfoTranslator.translate(args, agentDaemon);
        Assert.assertEquals(configuration, configInfo.getSecuredConfiguration());
        EasyMock.verify();
    }

    /**
     * Tests whether resource configuration are translated correctly.
     * 
     * @throws MissingConfigurationInfoExeption
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test
    public void translateConfigurationTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        Map<String, String> resourceConfiguration = createMockMap(100);
        Map<String, Map<String, String>> lwoConfiguration = createLwoConfigurationMap(25, 4);
        Map<String, String> unified = mergeResourceAndLwoConfigMap(resourceConfiguration, lwoConfiguration);

        Configuration_args args = createBaseConfigurationArgs();
        args.setConfiguration(unified);
        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfo configInfo = ConfigurationInfoTranslator.translate(args, agentDaemon);
        Assert.assertEquals(resourceConfiguration, configInfo.getConfiguration());
        EasyMock.verify();
    }

    /**
     * Tests whether LWO configuration are translated correctly.
     * 
     * @throws MissingConfigurationInfoExeption
     * @throws PluginException
     * @throws AgentRunningException
     */
    @Test
    public void translateLwoConfigurationTest()
        throws MissingConfigurationInfoExeption, AgentRunningException,
        PluginException {
        Map<String, String> resourceConfiguration = createMockMap(100);
        Map<String, Map<String, String>> lwoConfiguration = createLwoConfigurationMap(25, 4);
        Map<String, String> unified = mergeResourceAndLwoConfigMap(resourceConfiguration, lwoConfiguration);

        Configuration_args args = createBaseConfigurationArgs();
        args.setConfiguration(unified);
        EasyMock.expect(agentDaemon.getTypeInfo(RESOURCE_KIND, null)).andReturn(new PlatformTypeInfo()).anyTimes();
        EasyMock.replay(agentDaemon);
        ConfigurationInfo configInfo = ConfigurationInfoTranslator.translate(args, agentDaemon);
        Assert.assertEquals(lwoConfiguration, configInfo.getLwoConfiguration());
        EasyMock.verify();
    }

    private Configuration_args addSchedulings(Configuration_args args,
                                              List<String[]> schedulings) {
        for (String[] schedule : schedulings) {
            args.addScheduling(schedule[2], schedule[0], schedule[1]);
        }
        return args;
    }

    private Map<String, String> mergeResourceAndLwoConfigMap(Map<String, String> resourceConfiguartion,
                                                             Map<String, Map<String, String>> lwoConfiguration) {
        Map<String, String> unified = new HashMap<String, String>(resourceConfiguartion);
        for (Entry<String, Map<String, String>> lwoConfig : lwoConfiguration.entrySet()) {
            for (Entry<String, String> config : lwoConfig.getValue().entrySet()) {
                unified.put(lwoConfig.getKey() + "|" + config.getKey(), config.getValue());
            }
        }
        return unified;

    }

    private Map<String, Map<String, String>> createLwoConfigurationMap(int size,
                                                                       int numOfTypes) {
        Map<String, Map<String, String>> configuration = new HashMap<String, Map<String, String>>();
        for (int type = 0; type < numOfTypes; type++) {
            configuration.put("Type:Instance_" + type, createMockMap(size));
        }
        return configuration;
    }

    private Configuration_args createBaseConfigurationArgs() {
        Configuration_args args = new Configuration_args();
        args.setResourceKind(RESOURCE_KIND);
        args.setParentId(PARENT_ID);
        args.setMonitoredResourceId(MONITORED_RESOURCE_ID);
        args.setResourceInternalId(RESOURCE_INTERNAL_ID);
        return args;
    }

    private List<String[]> createMockScheduling() {
        List<String[]> schedulings = new ArrayList<String[]>(90);
        for (int i = 10; i < 99; i++) {
            schedulings.add(new String[] { Integer.toString(i), Integer.toString(i * 20), "Name_" + i });
        }
        return schedulings;
    }

    private List<String[]> createMockLwoScheduling() {
        List<String[]> schedulings = new ArrayList<String[]>(100);
        for (int type = 0; type < 8; type++) {
            String lwo = "Type:Instance_" + type;
            for (int i = 10 + type * 10; i < 10 + (type + 1) * 10; i++) {
                schedulings.add(new String[] { Integer.toString(i), Integer.toString(i * 20), lwo + "|Name_" + i });
            }
        }
        return schedulings;
    }

    private Map<String, String> createMockMap(int size) {
        Map<String, String> mock = new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            mock.put("Key_" + i, "Value_" + i);
        }
        return mock;
    }
}
