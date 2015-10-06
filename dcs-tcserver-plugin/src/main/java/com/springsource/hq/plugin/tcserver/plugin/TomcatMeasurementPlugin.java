// Copyright (c) 2009-2010 VMware, Inc.  All rights reserved.
package com.springsource.hq.plugin.tcserver.plugin;
/*
 Copyright (C) 2010-2014 Pivotal Software, Inc.


 All rights reserved. This program and the accompanying materials
 are made available under the terms of the under the Apache License,
 Version 2.0 (the "License"); you may not use this file except in compliance
 with the License. You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxMeasurementPlugin;
import org.hyperic.hq.product.jmx.MxUtil;

import com.springsource.hq.plugin.tcserver.plugin.utils.JmxUtils;
import com.springsource.hq.plugin.tcserver.plugin.utils.MxUtilJmxUtils;

/**
 * The measurement plugin that handles the custom measurements.
 *
 * @author Jennifer Hickey
 * @author jkonicki
 *
 */
public class TomcatMeasurementPlugin extends MxMeasurementPlugin {

    private static final String DEADLOCKED_DETECTION_COUNT = "deadlockedThreadCount";
    private static final String PERCENT_UP_TIME_IN_GARBAGE_COLLECTION_METRIC_NAME = "percentUpTimeSpent";
    private static final String PERCENT_ACTIVE_CONNECTIONS = "percentActiveConnections";
    private static final String PERCENT_ALLOCATED_THREADS = "percentAllocatedThreads";
    private static final String PERCENT_ACTIVE_THREADS = "percentActiveThreads";
    private static final String HEAP_FREE_MEMORY = "HeapMemoryUsage.free";
    private static final Log LOGGER = LogFactory.getLog(TomcatMeasurementPlugin.class);
    private final JmxUtils mxUtil;

    public TomcatMeasurementPlugin() {
        this(new MxUtilJmxUtils());
    }

    TomcatMeasurementPlugin(JmxUtils jmxUtils) {
        this.mxUtil = jmxUtils;
    }

    private MBeanServerConnection getConnection(Properties config) throws PluginException, MetricUnreachableException {
        try {
            return mxUtil.getMBeanServer(config);
        } catch (MalformedURLException e) {
            String msg = "Malformed URL: [" + config.getProperty(mxUtil.getJmxUrlProperty()) + "]";
            throw new PluginException(msg, e);
        } catch (IOException e) {
            String msg = "Can't connect to MBeanServer with url [" + config.getProperty(mxUtil.getJmxUrlProperty()) + "] and user ["
                    + config.getProperty(MxUtil.PROP_JMX_USERNAME) + "]: " + e;
            throw new MetricUnreachableException(msg, e);
        }
    }

    private MetricValue percentUptimeInGarbageCollection(final MBeanServerConnection connection) throws MetricUnreachableException,
            MetricNotFoundException, PluginException {
        double percentCollectionTime = 0;
        try {
            long totalGcTime = getTotalGarbageCollectionTime(connection);
            long upTime = getJvmUptime(connection);

            if (upTime > 0) {
                percentCollectionTime = (double) totalGcTime / (double) upTime;
            }
        } catch (IllegalArgumentException e) {
            // Common occurrence of this can happen when the user is using
            // an IBM JVM.
            LOGGER.debug("Unable to retrieve metric " + PERCENT_UP_TIME_IN_GARBAGE_COLLECTION_METRIC_NAME + e.getMessage(), e);
            throw new MetricUnreachableException("Unable to retrieve metric " + PERCENT_UP_TIME_IN_GARBAGE_COLLECTION_METRIC_NAME + " - "
                    + e.getMessage(), e);
        }
        return new MetricValue(percentCollectionTime * 100);
    }

    private int getDeadlockedThreadCount(MBeanServerConnection connection) throws MetricInvalidException, MetricUnreachableException {
        int deadlockCount = 0;
        try {
            // in version 1.6+ there is a findDeadlockedThreads method
            long[] deadlocks;
            try {
                deadlocks = (long[]) connection.invoke(new ObjectName("java.lang:type=Threading"), "findDeadlockedThreads", null, null);
            } catch (MalformedObjectNameException e) {
                throw new MetricInvalidException("Error querying for deadlock thread mbean: " + e.getMessage(), e);
            } catch (JMException e) {
                LOGGER.debug("Method 'findDeadlockedThreads' for objectname 'java.lang:type=Threading'"
                        + "was not found. Trying method findMonitorDeadlockedThreads...", e);
                // If this occurs then the issue is most likely related to the
                // Java version.
                // Now check for the method on java 1.5
                try {
                    deadlocks = (long[]) connection.invoke(new ObjectName("java.lang:type=Threading"), "findMonitorDeadlockedThreads", null, null);
                } catch (MalformedObjectNameException me) {
                    throw new MetricInvalidException("Error querying for deadlock thread mbean: " + me.getMessage(), me);
                } catch (JMException ex) {
                    LOGGER.debug("Unable to retrieve DeadlockedThreads count: ", ex);
                    throw new MetricUnreachableException("Unable to reach deadlock thread mbean: " + ex.getMessage(), ex);
                }
            }

            if (deadlocks != null) {
                deadlockCount = deadlocks.length;
            }
            return deadlockCount;
        } catch (IOException e) {
            throw new MetricUnreachableException("Error querying for deadlock thread mbean: " + e.getMessage(), e);
        }
    }

    // Commented out for TCS-71
    // private GarbageCollectorMXBean getGarbageCollectorMXBean(
    // MBeanServerConnection connection, ObjectName garbageCollector)
    // throws IOException {
    // return ManagementFactory.newPlatformMXBeanProxy(connection,
    // garbageCollector.toString(), GarbageCollectorMXBean.class);
    //
    // }
    private long getJvmUptime(MBeanServerConnection connection) throws MetricUnreachableException {
        String upTimeQuery = "java.lang:type=Runtime";
        RuntimeMXBean bean;
        try {
            bean = getRuntimeMXBean(connection, upTimeQuery);
        } catch (IOException e) {
            throw new MetricUnreachableException("Error obtaining process UpTime:" + e.getMessage(), e);
        }
        return bean.getUptime();
    }

    private RuntimeMXBean getRuntimeMXBean(MBeanServerConnection connection, String upTimeQuery) throws IOException {
        return ManagementFactory.newPlatformMXBeanProxy(connection, upTimeQuery, RuntimeMXBean.class);
    }

    private long getTotalGarbageCollectionTime(MBeanServerConnection connection) throws MetricUnreachableException, MetricNotFoundException,
            PluginException {

        long totalGcTimeMillis = 0;

        try {

            // Use of the MXBean replaced by plain old JMX query for TCS-71
            //
            // Set<ObjectName> garbageCollectors = connection.queryNames(
            // new ObjectName("java.lang:type=GarbageCollector,*"), null);
            // for (ObjectName garbageCollectorName : garbageCollectors) {
            // GarbageCollectorMXBean garbageCollector = getGarbageCollectorMXBean(
            // connection, garbageCollectorName);
            // long collectionTime = garbageCollector.getCollectionTime();
            ObjectName gcObjName = new ObjectName("java.lang:type=GarbageCollector,*");
            Set<ObjectInstance> garbageCollectors = connection.queryMBeans(gcObjName, null);

            for (ObjectInstance instance : garbageCollectors) {
                ObjectName instanceName = instance.getObjectName();
                Long collectionTime = (Long) connection.getAttribute(instance.getObjectName(), "CollectionTime");
                LOGGER.debug(instanceName + "::CollectionTime=" + collectionTime);
                if (collectionTime > -1) {
                    totalGcTimeMillis += collectionTime;
                }
            }
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException("Error querying for GarbageCollector MBeans: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MetricUnreachableException("Error querying for GarbageCollector MBeans:" + e.getMessage(), e);
        } catch (AttributeNotFoundException e) {
            throw new MetricNotFoundException("Error querying for GarbageCollector MBeans:" + e.getMessage(), e);
        } catch (InstanceNotFoundException e) {
            throw new MetricNotFoundException("Error querying for GarbageCollector MBeans:" + e.getMessage(), e);
        } catch (MBeanException e) {
            throw new PluginException("Error querying for GarbageCollector MBeans:" + e.getMessage(), e);
        } catch (ReflectionException e) {
            throw new PluginException("Error querying for GarbageCollector MBeans:" + e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new PluginException("Error querying for GarbageCollector MBeans:" + e.getMessage(), e);
        }

        return totalGcTimeMillis;
    }

    private long getFreeHeapMemory(MBeanServerConnection connection) throws MetricUnreachableException {
        try {
            MemoryMXBean memoryBean = ManagementFactory.newPlatformMXBeanProxy(connection, "java.lang:type=Memory", MemoryMXBean.class);
            long max = memoryBean.getHeapMemoryUsage().getMax();
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            return max - used;
        } catch (IOException e) {
            throw new MetricUnreachableException("Error retrieving Memory MBean" + e.getMessage(), e);
        }
    }

    private MetricValue getPercentActiveConnections(MBeanServerConnection connection, Metric metric) throws MetricUnreachableException,
            MetricNotFoundException, PluginException {
        int numActiveConnections = 0;
        int maxActiveConnections = 0;

        try {
            ObjectName dataSourceObjectName = new ObjectName(metric.getObjectName());
            numActiveConnections = ((Integer) connection.getAttribute(dataSourceObjectName, "numActive"));
            maxActiveConnections = ((Integer) connection.getAttribute(dataSourceObjectName, "maxActive"));
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException("Error querying for DataSource MBeans: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MetricUnreachableException("Error querying for DataSource MBeans:" + e.getMessage(), e);
        } catch (AttributeNotFoundException e) {
            throw new MetricNotFoundException("Error querying for DataSource MBeans:" + e.getMessage(), e);
        } catch (InstanceNotFoundException e) {
            throw new MetricNotFoundException("Error querying for DataSource MBeans:" + e.getMessage(), e);
        } catch (MBeanException e) {
            throw new PluginException("Error querying for DataSource MBeans:" + e.getMessage(), e);
        } catch (ReflectionException e) {
            throw new PluginException("Error querying for DataSource MBeans:" + e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new PluginException("Error querying for DataSource MBeans:" + e.getMessage(), e);
        }

        return new MetricValue(100d * (double) numActiveConnections / (double) maxActiveConnections);
    }

    private MetricValue getPercentAllocatedThreads(MBeanServerConnection connection, Metric metric) throws MetricUnreachableException,
            MetricNotFoundException, PluginException {
        int currentThreadCount = 0;
        int maxThreads = 0;

        try {
            ObjectName threadPoolObjectName = new ObjectName(metric.getObjectName());
            currentThreadCount = ((Integer) connection.getAttribute(threadPoolObjectName, "currentThreadCount"));
            maxThreads = ((Integer) connection.getAttribute(threadPoolObjectName, "maxThreads"));
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException("Error querying for Thread Pool MBeans: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MetricUnreachableException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (AttributeNotFoundException e) {
            throw new MetricNotFoundException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (InstanceNotFoundException e) {
            throw new MetricNotFoundException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (MBeanException e) {
            throw new PluginException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (ReflectionException e) {
            throw new PluginException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new PluginException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        }

        return new MetricValue(100d * (double) currentThreadCount / (double) maxThreads);
    }

    private MetricValue getPercentActiveThreads(MBeanServerConnection connection, Metric metric) throws MetricUnreachableException,
            MetricNotFoundException, PluginException {
        int currentThreadsBusy = 0;
        int maxThreads = 0;

        try {
            ObjectName threadPoolObjectName = new ObjectName(metric.getObjectName());
            currentThreadsBusy = ((Integer) connection.getAttribute(threadPoolObjectName, "currentThreadsBusy"));
            maxThreads = ((Integer) connection.getAttribute(threadPoolObjectName, "maxThreads"));
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException("Error querying for Thread Pool MBeans: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MetricUnreachableException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (AttributeNotFoundException e) {
            throw new MetricNotFoundException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (InstanceNotFoundException e) {
            throw new MetricNotFoundException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (MBeanException e) {
            throw new PluginException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (ReflectionException e) {
            throw new PluginException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new PluginException("Error querying for Thread Pool MBeans:" + e.getMessage(), e);
        }

        return new MetricValue(100d * (double) currentThreadsBusy / (double) maxThreads);
    }

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res;
        try {
            if (PERCENT_UP_TIME_IN_GARBAGE_COLLECTION_METRIC_NAME.equals(metric.getAttributeName())) {
                res = new MetricValue(percentUptimeInGarbageCollection(getConnection(metric.getProperties())));
            } else if (DEADLOCKED_DETECTION_COUNT.equals(metric.getAttributeName())) {
                res = new MetricValue(getDeadlockedThreadCount(getConnection(metric.getProperties())));
            } else if (HEAP_FREE_MEMORY.equals(metric.getAttributeName())) {
                res = new MetricValue(getFreeHeapMemory(getConnection(metric.getProperties())));
            } else if (PERCENT_ACTIVE_CONNECTIONS.equals(metric.getAttributeName())) {
                res = new MetricValue(getPercentActiveConnections(getConnection(metric.getProperties()), metric));
            } else if (PERCENT_ALLOCATED_THREADS.equals(metric.getAttributeName())) {
                res = new MetricValue(getPercentAllocatedThreads(getConnection(metric.getProperties()), metric));
            } else if (PERCENT_ACTIVE_THREADS.equals(metric.getAttributeName())) {
                res = new MetricValue(getPercentActiveThreads(getConnection(metric.getProperties()), metric));
            } else {
                res = super.getValue(metric);
            }
        } catch (SecurityException e) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(e,e);
            } else {
                LOGGER.error(e);
            }
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN);
            } else {
                res = MetricValue.NONE;
            }
        }

        if (metric.isAvail()) {
            LOGGER.debug("[getValue] res:" + res + " metric:" + metric);
        }
        
        return res;
    }
}
