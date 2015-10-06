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

package com.vmware.epops.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.product.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.epops.plugin.model.PluginResourceType;
import com.vmware.epops.plugin.model.ResourceTypeConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class PluginLoaderTest {

    private static final int RESOURCES = 141;
    private static final int LIGHT_RESOURCES = 101;

    @Autowired
    private PluginLoader pluginLoader;

    private Collection<PluginResourceType> allResourceTypes;
    private static String PLUGINS_FOLDER = "src/test/resources/plugins/";
    private static List<String> activePluginFileNames = Arrays.asList(PLUGINS_FOLDER + "activemq-plugin.jar",
                PLUGINS_FOLDER + "mssql-plugin.jar",
                PLUGINS_FOLDER + "bind-plugin.jar",
                PLUGINS_FOLDER + "hibernate-services.xml",
                PLUGINS_FOLDER + "hq-internal-plugin.xml",
                PLUGINS_FOLDER + "jmx-plugin.xml",
                PLUGINS_FOLDER + "jvm-jmx-metrics.xml",
                PLUGINS_FOLDER + "multi-process-metrics.xml",
                PLUGINS_FOLDER + "process-metrics.xml",
                PLUGINS_FOLDER + "resin-plugin.xml",
                PLUGINS_FOLDER + "sendmail-plugin.xml",
                PLUGINS_FOLDER + "sun-jvm-service.xml",
                PLUGINS_FOLDER + "system-plugin.jar",
                PLUGINS_FOLDER + "tomcat-plugin.jar",
                PLUGINS_FOLDER + "weather-plugin.jar");

    @Before
    public void init()
        throws PluginException {
        allResourceTypes = pluginLoader.getModel(activePluginFileNames);
    }

    @Test
    public void testGetAllPluginResourceTypes()
    {
        Assert.assertNotNull(allResourceTypes);
        /* According to current plugins files in test resource - there should be a fixed number of PluginResourceType
        please be aware that any change in the plugins folder (add/remove) can change this number and lead to
        breaking this test. */

        for (PluginResourceType pluginResourceType : allResourceTypes) {
            if (!expectedResourceList().contains(pluginResourceType.getName())) {
                System.out.println("Unexpected resource found: " + pluginResourceType.getName());
            }
        }

        Set<String> light = new HashSet<>();
        Assert.assertEquals("Resources number is not as expected", RESOURCES, allResourceTypes.size());
        int lightResources = 0;
        for (PluginResourceType pluginResourceType : allResourceTypes) {
            if (pluginResourceType.isLight()) {
                lightResources++;
                light.add(pluginResourceType.getName());
            }
        }
        Assert.assertEquals("Light resources number is no as expected", LIGHT_RESOURCES, lightResources);
    }

    @Test
    public void testResourceTypeConfiguration() {

        List<ResourceTypeConfig> configProperties = new ArrayList<ResourceTypeConfig>();

        for (PluginResourceType pluginResourceType : allResourceTypes) {
            if (pluginResourceType.getName().equals("Apache Tomcat")) {
                configProperties = pluginResourceType.getConfigProperties();
            }
        }
        Assert.assertEquals("Configuration fields number", 7, configProperties.size());
        ResourceTypeConfig resourceTypeConfig = configProperties.get(2);
        Assert.assertEquals("jmx.username", resourceTypeConfig.getName());
        Assert.assertEquals("JMX username", resourceTypeConfig.getDescription());
        Assert.assertEquals("string", resourceTypeConfig.getType());
        Assert.assertTrue(resourceTypeConfig.isMandatory());
        Assert.assertNull("The default value should not be visible, this configuration is sensitive.",
                    resourceTypeConfig.getDefaultValue());
    }

    @Test
    public void testResourceTypeDiscoverableProps() {

        List<ResourceTypeConfig> configProperties = new ArrayList<ResourceTypeConfig>();

        for (PluginResourceType pluginResourceType : allResourceTypes) {
            if (pluginResourceType.getName().equals("Linux")) {
                configProperties = pluginResourceType.getDiscoverableProperties();
            }
        }
        Assert.assertEquals("Discoverable proprties number", 17, configProperties.size());
    }

    @Test
    public void testResourceTypeMetadata() {

        for (PluginResourceType pluginResourceType : allResourceTypes) {
            switch (pluginResourceType.getName()) {
                case "Linux": // Linux platform
                    Assert.assertEquals("Linux should not be light resource", false, pluginResourceType.isLight());
                    Assert.assertEquals("Linux should not be virtual resource", false, pluginResourceType.isVirtual());
                    Assert.assertEquals("Linux should not be remote resource", false, pluginResourceType.isRemote());
                    Assert.assertEquals("Linux should not have parents", 0, pluginResourceType.getParents().size());
                    break;
                case "Windows": // Windows platform
                    Assert.assertEquals("Windows should not be light resource", false, pluginResourceType.isLight());
                    Assert.assertEquals("Windows should not be virtual resource", false, pluginResourceType.isVirtual());
                    Assert.assertEquals("Windows should not be remote resource", false, pluginResourceType.isRemote());
                    Assert.assertEquals("Windows should not have parents", 0, pluginResourceType.getParents().size());
                    break;
                case "ActiveMQ 4.0": // server
                    Assert.assertEquals("ActiveMQ 4.0 should not be light resource", false,
                                pluginResourceType.isLight());
                    Assert.assertEquals("ActiveMQ 4.0 should not be virtual resource", false,
                                pluginResourceType.isVirtual());
                    Assert.assertEquals("ActiveMQ 4.0 should not be remote resource", false,
                                pluginResourceType.isRemote());
                    Assert.assertEquals("ActiveMQ 4.0 should have 5 parents", 5,
                                pluginResourceType.getParents().size());
                    break;
                case "FileServer": // virtual server
                    Assert.assertEquals("FileServer should not be light resource", false, pluginResourceType.isLight());
                    Assert.assertEquals("FileServer should be virtual resource", true, pluginResourceType.isVirtual());
                    Assert.assertEquals("FileServer should not be remote resource", false,
                                pluginResourceType.isRemote());
                    Assert.assertEquals("FileServer should have 5 parents", 5, pluginResourceType.getParents().size());
                    break;
                case "Apache Tomcat 6.0 Cache": // service
                    Assert.assertEquals("Apache Tomcat 6.0 Cache should be light resource", true,
                                pluginResourceType.isLight());
                    Assert.assertEquals("Apache Tomcat 6.0 Cache should not be virtual resource", false,
                                pluginResourceType.isVirtual());
                    Assert.assertEquals("Apache Tomcat 6.0 Cache should not be remote resource", false,
                                pluginResourceType.isRemote());
                    Assert.assertEquals("Apache Tomcat 6.0 Cache should have 1 parent", 1,
                                pluginResourceType.getParents().size());
                    break;
                case "Process": // platform service
                    Assert.assertEquals("Process should be light resource", true, pluginResourceType.isLight());
                    Assert.assertEquals("Process should not be virtual resource", false, pluginResourceType.isVirtual());
                    Assert.assertEquals("Process should not be remote resource", false, pluginResourceType.isRemote());
                    Assert.assertEquals("Process should have 5 parents", 5, pluginResourceType.getParents().size());
                    break;
            }
        }
    }

    private List<String> expectedResourceList() {
        List<String> plug = new ArrayList<>();
        plug.add("Linux");
        plug.add("Windows");
        plug.add("Solaris");
        plug.add("AIX");
        plug.add("HPUX");
        plug.add("FileServer");
        plug.add("NetworkServer");
        plug.add("ProcessServer");
        plug.add("WindowsServer");
        plug.add("Windows Service");
        plug.add("Apache Tomcat");
        plug.add("JBoss Web 2.0");
        plug.add("JBoss Web 2.1");
        plug.add("JBoss Web 3.0");
        plug.add("ActiveMQ 4.0");
        plug.add("ActiveMQ 5.0");
        plug.add("ActiveMQ 5.1");
        plug.add("ActiveMQ 5.2");
        plug.add("ActiveMQ 5.3");
        plug.add("ActiveMQ 5.4");
        plug.add("ActiveMQ 5.5");
        plug.add("ActiveMQ 5.6");
        plug.add("ActiveMQ 5.7");
        plug.add("ActiveMQ 5.8");
        plug.add("ActiveMQ Embedded 5.0");
        plug.add("ActiveMQ Embedded 5.1");
        plug.add("ActiveMQ Embedded 5.2");
        plug.add("ActiveMQ Embedded 5.3");
        plug.add("ActiveMQ Embedded 5.4");
        plug.add("ActiveMQ Embedded 5.5");
        plug.add("ActiveMQ Embedded 5.6");
        plug.add("ActiveMQ Embedded 5.7");
        plug.add("ActiveMQ Embedded 5.8");
        plug.add("Sun JVM 1.5");
        plug.add("Sendmail 8.x");
        plug.add("Resin 3.x");
        plug.add("Bind 9.x");
        plug.add("JBoss 4.0 HQ Internals");
        plug.add("JBoss 4.2 HQ Internals");
        plug.add("Country");
        plug.add("Process");
        plug.add("MultiProcess");
        plug.add("FileServer Mount");
        plug.add("Apache Tomcat Cache");
        plug.add("Apache Tomcat 7.0 DataSource Pool");
        plug.add("Apache Tomcat 5.5 & 6.0 DataSource Pool");
        plug.add("Apache Tomcat Global Request Processor");
        plug.add("Apache Tomcat HTTP");
        plug.add("Apache Tomcat Java Process Metrics");
        plug.add("Apache Tomcat JSP Monitor");
        plug.add("Apache Tomcat Servlet Monitor");
        plug.add("Apache Tomcat Thread Pools");
        plug.add("Apache Tomcat Web Module Stats");
        plug.add("JBoss Web 3.0 Cache");
        plug.add("JBoss Web 3.0 DataSource Pool");
        plug.add("JBoss Web 3.0 Global Request Processor");
        plug.add("JBoss Web 3.0 Hibernate Session Factory");
        plug.add("JBoss Web 3.0 HQ Internals");
        plug.add("JBoss Web 3.0 HTTP");
        plug.add("JBoss Web 3.0 Hyperic Data Source");
        plug.add("JBoss Web 3.0 Java Process Metrics");
        plug.add("JBoss Web 3.0 JSP Monitor");
        plug.add("JBoss Web 3.0 Servlet Monitor");
        plug.add("JBoss Web 3.0 Thread Pools");
        plug.add("JBoss Web 3.0 Web Module Stats");
        plug.add("CPU");
        plug.add("FileServer Block Device");
        plug.add("FileServer Logical Disk");
        plug.add("FileServer Directory");
        plug.add("FileServer Directory Tree");
        plug.add("FileServer File");
        plug.add("FileServer Physical Disk");
        plug.add("NetworkServer Interface");
        plug.add("Script");
        plug.add("Service");
        plug.add("Sun JVM 1.5 Garbage Collector");
        plug.add("Sun JVM 1.5 Memory Pool");
        plug.add("JBoss Web 2.1 Cache");
        plug.add("JBoss Web 2.1 DataSource Pool");
        plug.add("JBoss Web 2.1 Global Request Processor");
        plug.add("JBoss Web 2.1 Hibernate Session Factory");
        plug.add("JBoss Web 2.1 HQ Internals");
        plug.add("JBoss Web 2.1 HTTP");
        plug.add("JBoss Web 2.1 Hyperic Data Source");
        plug.add("JBoss Web 2.1 Java Process Metrics");
        plug.add("JBoss Web 2.1 JSP Monitor");
        plug.add("JBoss Web 2.1 Servlet Monitor");
        plug.add("JBoss Web 2.1 Thread Pools");
        plug.add("JBoss Web 2.1 Web Module Stats");
        plug.add("ActiveMQ Embedded 5.0 Broker");
        plug.add("ActiveMQ Embedded 5.0 Connector");
        plug.add("ActiveMQ Embedded 5.0 Queue");
        plug.add("ActiveMQ Embedded 5.0 Topic");
        plug.add("JBoss Web 2.0 Cache");
        plug.add("JBoss Web 2.0 DataSource Pool");
        plug.add("JBoss Web 2.0 Global Request Processor");
        plug.add("JBoss Web 2.0 Hibernate Session Factory");
        plug.add("JBoss Web 2.0 HQ Internals");
        plug.add("JBoss Web 2.0 HTTP");
        plug.add("JBoss Web 2.0 Hyperic Data Source");
        plug.add("JBoss Web 2.0 Java Process Metrics");
        plug.add("JBoss Web 2.0 JSP Monitor");
        plug.add("JBoss Web 2.0 Servlet Monitor");
        plug.add("JBoss Web 2.0 Thread Pools");
        plug.add("JBoss Web 2.0 Web Module Stats");
        plug.add("ActiveMQ 4.0 Broker");
        plug.add("ActiveMQ 4.0 Connector");
        plug.add("ActiveMQ 4.0 Queue");
        plug.add("ActiveMQ 4.0 Topic");
        plug.add("Sendmail 8.x Message Submission Process");
        plug.add("Sendmail 8.x Root Daemon Process");
        plug.add("Sendmail 8.x SMTP");
        plug.add("Resin 3.x Connection Pool");
        plug.add("Resin 3.x Port");
        plug.add("Resin 3.x Webapp");
        plug.add("ActiveMQ Embedded 5.1 Broker");
        plug.add("ActiveMQ Embedded 5.1 Connector");
        plug.add("ActiveMQ Embedded 5.1 Queue");
        plug.add("ActiveMQ Embedded 5.1 Topic");
        plug.add("ActiveMQ Embedded 5.2 Broker");
        plug.add("ActiveMQ Embedded 5.2 Connector");
        plug.add("ActiveMQ Embedded 5.2 Queue");
        plug.add("ActiveMQ Embedded 5.2 Topic");
        plug.add("ActiveMQ Embedded 5.3 Broker");
        plug.add("ActiveMQ Embedded 5.3 Connector");
        plug.add("ActiveMQ Embedded 5.3 Queue");
        plug.add("ActiveMQ Embedded 5.3 Topic");
        plug.add("ActiveMQ Embedded 5.4 Broker");
        plug.add("ActiveMQ Embedded 5.4 Connector");
        plug.add("ActiveMQ Embedded 5.4 Queue");
        plug.add("ActiveMQ Embedded 5.4 Topic");
        plug.add("Country Town");
        plug.add("ActiveMQ Embedded 5.5 Broker");
        plug.add("ActiveMQ Embedded 5.5 Connector");
        plug.add("ActiveMQ Embedded 5.5 Queue");
        plug.add("ActiveMQ Embedded 5.5 Topic");
        plug.add("ActiveMQ Embedded 5.6 Broker");
        plug.add("ActiveMQ Embedded 5.6 Connector");
        plug.add("ActiveMQ Embedded 5.6 Queue");
        plug.add("ActiveMQ Embedded 5.6 Topic");
        plug.add("ActiveMQ Embedded 5.7 Broker");
        plug.add("ActiveMQ Embedded 5.7 Connector");
        plug.add("ActiveMQ Embedded 5.7 Queue");
        plug.add("ActiveMQ Embedded 5.7 Topic");
        plug.add("ActiveMQ Embedded 5.8 Broker");
        plug.add("ActiveMQ Embedded 5.8 Connector");
        plug.add("ActiveMQ Embedded 5.8 Queue");
        plug.add("ActiveMQ Embedded 5.8 Topic");
        plug.add("ActiveMQ 5.0 Broker");
        plug.add("ActiveMQ 5.0 Connector");
        plug.add("ActiveMQ 5.0 Queue");
        plug.add("ActiveMQ 5.0 Topic");
        plug.add("ActiveMQ 5.1 Broker");
        plug.add("ActiveMQ 5.1 Connector");
        plug.add("ActiveMQ 5.1 Queue");
        plug.add("ActiveMQ 5.1 Topic");
        plug.add("ActiveMQ 5.2 Broker");
        plug.add("ActiveMQ 5.2 Connector");
        plug.add("ActiveMQ 5.2 Queue");
        plug.add("ActiveMQ 5.2 Topic");
        plug.add("ActiveMQ 5.3 Broker");
        plug.add("ActiveMQ 5.3 Connector");
        plug.add("ActiveMQ 5.3 Queue");
        plug.add("ActiveMQ 5.3 Topic");
        plug.add("ActiveMQ 5.4 Broker");
        plug.add("ActiveMQ 5.4 Connector");
        plug.add("ActiveMQ 5.4 Queue");
        plug.add("ActiveMQ 5.4 Topic");
        plug.add("ActiveMQ 5.5 Broker");
        plug.add("ActiveMQ 5.5 Connector");
        plug.add("ActiveMQ 5.5 Queue");
        plug.add("ActiveMQ 5.5 Topic");
        plug.add("ActiveMQ 5.6 Broker");
        plug.add("ActiveMQ 5.6 Connector");
        plug.add("ActiveMQ 5.6 Queue");
        plug.add("ActiveMQ 5.6 Topic");
        plug.add("ActiveMQ 5.7 Broker");
        plug.add("ActiveMQ 5.7 Connector");
        plug.add("ActiveMQ 5.7 Queue");
        plug.add("ActiveMQ 5.7 Topic");
        plug.add("ActiveMQ 5.8 Broker");
        plug.add("ActiveMQ 5.8 Connector");
        plug.add("ActiveMQ 5.8 Queue");
        plug.add("ActiveMQ 5.8 Topic");

        return plug;
    }

}
