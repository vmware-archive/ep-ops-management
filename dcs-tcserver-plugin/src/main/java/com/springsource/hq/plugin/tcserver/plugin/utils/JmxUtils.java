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

import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public interface JmxUtils {

    MBeanServerConnection getMBeanServer(Properties configProperties) throws MalformedURLException, IOException;

    Object getValue(Properties configProperties, String appObjectName, String string) throws MalformedObjectNameException,
        AttributeNotFoundException, InstanceNotFoundException, MalformedURLException, MBeanException, ReflectionException, PluginException,
        IOException;

    Object invoke(Properties configProperties, String objectName, String string, Object[] objects, String[] strings)
        throws MetricUnreachableException, MetricNotFoundException, PluginException;

    String getJmxUrlProperty();

    boolean checkConnection(ConfigResponse config);

}
