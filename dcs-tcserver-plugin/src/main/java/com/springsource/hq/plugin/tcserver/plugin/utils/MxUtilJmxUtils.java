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

package com.springsource.hq.plugin.tcserver.plugin.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public final class MxUtilJmxUtils implements JmxUtils {

    private final Log log = LogFactory.getLog(MxUtilJmxUtils.class);

    public MBeanServerConnection getMBeanServer(Properties configProperties) throws MalformedURLException, IOException {
        return MxUtil.getMBeanServer(configProperties);
    }

    public Object getValue(Properties configProperties, String appObjectName, String string) throws MalformedObjectNameException,
        AttributeNotFoundException, InstanceNotFoundException, MalformedURLException, MBeanException, ReflectionException, PluginException,
        IOException {
        return MxUtil.getValue(configProperties, appObjectName, string);
    }

    public Object invoke(Properties configProperties, String objectName, String string, Object[] objects, String[] strings)
        throws MetricUnreachableException, MetricNotFoundException, PluginException {
        return MxUtil.invoke(configProperties, objectName, string, objects, strings);
    }

    public String getJmxUrlProperty() {
        return MxUtil.PROP_JMX_URL;
    }

    public boolean checkConnection(ConfigResponse config) {
        JMXConnector jmxConnector = null;

        try {
            jmxConnector = MxUtil.getMBeanConnector(config.toProperties());
            return true;
        } catch (IOException ioe) {
            log.warn("Connection check failed", ioe);
            return false;
        } finally {
            if (jmxConnector != null) {
                try {
                    jmxConnector.close();
                } catch (IOException ioe) {
                    log.warn("Failed to close connection following check", ioe);
                }
            }
        }
    }
}
