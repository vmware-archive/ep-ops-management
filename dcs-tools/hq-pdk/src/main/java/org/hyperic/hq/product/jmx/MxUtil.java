/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.product.jmx;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;
import javax.management.j2ee.statistics.TimeStatistic;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.util.config.ConfigResponse;

public class MxUtil {

    static final String PTQL_PREFIX = "ptql:";

    public static final String PROP_JMX_URL = "jmx.url";
    public static final String PROP_JMX_PORT = "jmx.port";
    public static final String PROP_JMX_USERNAME = "jmx.username";
    public static final String PROP_JMX_PASSWORD = "jmx.password";
    public static final String PROP_JMX_PROVIDER_PKGS = "jmx.provider.pkgs";

    private static final String STATS_PREFIX = "Stats.";
    private static final String COMPOSITE_PREFIX = "Composite.";

    private static final Log log = LogFactory.getLog(MxUtil.class);
    private static HashMap cache = new HashMap();

    // expand Foo=* -> Foo=%Foo%
    static String expandObjectName(String name) {
        int ix = name.indexOf("*");
        if (ix == -1) {
            return name;
        }

        StringBuffer objectName = new StringBuffer();

        ix = name.indexOf(':');
        if (ix != -1) {
            objectName.append(name.substring(0, ix + 1));
            name = name.substring(ix + 1); // skip domain
        }

        StringTokenizer tok = new StringTokenizer(name, ",");

        while (tok.hasMoreTokens()) {
            String pair = tok.nextToken();
            if (pair.equals("*")) {
                objectName.append(pair);
                break;
            }
            ix = pair.indexOf("=");
            if (ix == -1) {
                throw new IllegalArgumentException(name);
            }

            String key = pair.substring(0, ix);
            String val = pair.substring(ix + 1);
            // domain:type=Foo,name=*
            if (val.equals("*")) {
                val = "%" + key + "%";
            }
            objectName.append(key).append('=').append(val);
            if (tok.hasMoreTokens()) {
                objectName.append(',');
            }
        }

        return objectName.toString();
    }

    static String expandObjectName(String name,
                                   ConfigResponse config) {

        return Metric.translate(expandObjectName(name), config);
    }

    static String expandObjectName(String name,
                                   Properties config) {

        return Metric.translate(expandObjectName(name), config);
    }

    private static MetricInvalidException invalidObjectName(String name,
                                                            Exception e) {
        String msg =
                    "Malformed ObjectName [" + name + "]";
        return new MetricInvalidException(msg, e);
    }

    private static MetricUnreachableException unreachable(Properties props,
                                                          Exception e) {
        String msg =
                    "Can't connect to MBeanServer url [" + props.getProperty(PROP_JMX_URL) + "] "
                                + "port [" + props.getProperty(PROP_JMX_PORT) + "] "
                                + "using given credentials. Reason: " + e;
        return new MetricUnreachableException(msg, e);
    }

    private static MetricNotFoundException objectNotFound(String name,
                                                          Exception e) {
        String msg =
                    "ObjectName not found [" + name + "]: " + e;
        return new MetricNotFoundException(msg, e);
    }

    private static MetricNotFoundException attributeNotFound(String object,
                                                             String name,
                                                             Exception e) {
        String msg =
                    "Attribute not found [" + object + ":" + name + "]: " + e;
        return new MetricNotFoundException(msg, e);
    }

    private static PluginException error(String name,
                                         Exception e) {
        String msg =
                    "Invocation error [" + name + "]: " + e;
        return new PluginException(msg, e);
    }

    private static PluginException invalidURL(Properties props,
                                              Exception e) {
        String msg =
                    "Malformed URL: [" +
                                props.getProperty(MxUtil.PROP_JMX_URL) +
                                "]";
        return new PluginException(msg, e);
    }

    private static PluginException error(String name,
                                         Exception e,
                                         String method) {
        String msg =
                    "Method '" + method +
                                "' invocation error [" + name + "]: " + e;
        return new PluginException(msg, e);
    }

    static Double getJSR77Statistic(MBeanServerConnection mServer,
                                    ObjectName objName,
                                    String attribute)
        throws MalformedURLException,
        MalformedObjectNameException,
        IOException,
        MBeanException,
        AttributeNotFoundException,
        InstanceNotFoundException,
        ReflectionException,
        PluginException {

        Stats stats;
        Boolean provider =
                    (Boolean) mServer.getAttribute(objName, "statisticsProvider");
        if ((provider == null) || !provider.booleanValue()) {
            String msg =
                        objName + " does not provide statistics";
            throw new PluginException(msg);
        }

        stats = (Stats) mServer.getAttribute(objName, "stats");

        if (stats == null) {
            throw new PluginException(objName + " has no stats");
        }

        String statName =
                    attribute.substring(STATS_PREFIX.length());
        Statistic stat = stats.getStatistic(statName);
        if (stat == null) {
            String msg =
                        "Statistic '" + statName + "' not found [" + objName + "]";
            throw new AttributeNotFoundException(msg);
        }

        long value;
        if (stat instanceof CountStatistic) {
            value = ((CountStatistic) stat).getCount();
        }
        else if (stat instanceof RangeStatistic) {
            value = ((RangeStatistic) stat).getCurrent();
        }
        else if (stat instanceof TimeStatistic) {
            // get the average time
            long count = ((TimeStatistic) stat).getCount();
            if (count == 0)
                value = 0;
            else
                value = ((TimeStatistic) stat).getTotalTime() / count;
        }
        else {
            String msg =
                        "Unsupported statistic type [" +
                                    statName.getClass().getName() +
                                    " for [" + objName + ":" + attribute + "]";
            throw new MetricInvalidException(msg);
        }

        // XXX: handle bug with geronimo uptime metric
        if (statName.equals("UpTime")) {
            value = System.currentTimeMillis() - value;
        }

        return new Double(value);
    }

    // e.g. "Composite.Usage.committed"
    static Object getCompositeMetric(MBeanServerConnection mServer,
                                     ObjectName objName,
                                     String attribute)
        throws MalformedURLException,
        MalformedObjectNameException,
        IOException,
        MBeanException,
        AttributeNotFoundException,
        InstanceNotFoundException,
        ReflectionException,
        PluginException {

        String name =
                    attribute.substring(COMPOSITE_PREFIX.length());

        int ix = name.indexOf('.');
        if (ix == -1) {
            throw new MetricInvalidException("Missing composite key");
        }

        String attr = name.substring(0, ix);
        String key = name.substring(ix + 1);

        Object obj = mServer.getAttribute(objName, attr);
        if (obj instanceof CompositeData) {
            return MxCompositeData.getValue((CompositeData) obj, key);
        }
        else {
            throw new MetricInvalidException("Not CompositeData");
        }
    }

    static Object getValue(Metric metric)
        throws MetricNotFoundException,
        MetricInvalidException,
        MetricUnreachableException,
        PluginException
    {
        String objectName = Metric.decode(metric.getObjectName());
        String attribute = metric.getAttributeName();
        Properties config = metric.getProperties();

        try {
            return getValue(config, objectName, attribute);
        } catch (MalformedURLException e) {
            throw invalidURL(metric.getProperties(), e);
        } catch (MalformedObjectNameException e) {
            throw invalidObjectName(objectName, e);
        } catch (IOException e) {
            removeMBeanConnector(config);
            if (metric.isAvail()) {
                return new Double(Metric.AVAIL_DOWN);
            }
            throw unreachable(metric.getProperties(), e);
        } catch (MBeanException e) {
            throw error(metric.toString(), e);
        } catch (AttributeNotFoundException e) {
            // XXX not all MBeans have a reasonable attribute to
            // determine availability, so just assume if we get this far
            // the MBean exists and is alive.
            if (metric.isAvail()) {
                return new Double(Metric.AVAIL_UP);
            }
            throw attributeNotFound(objectName,
                        metric.getAttributeName(), e);
        } catch (InstanceNotFoundException e) {
            if (metric.isAvail()) {
                return new Double(Metric.AVAIL_DOWN);
            }
            throw objectNotFound(objectName, e);
        } catch (UndeclaredThrowableException e) {
            Throwable cause1 = e.getCause();
            if (cause1 instanceof InvocationTargetException) {
                Throwable cause2 = cause1.getCause();
                if (cause2 instanceof InstanceNotFoundException) {
                    throw objectNotFound(objectName, (InstanceNotFoundException) cause2);
                }
            }

            throw e;
        } catch (ReflectionException e) {
            throw error(metric.toString(), e);

        } catch (RuntimeException e) {
            // Temporary fix until availability strings can be mapped
            // in hq-plugin.xml. Resin wraps AttributeNotFoundException
            if (metric.isAvail()) {
                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof AttributeNotFoundException) {
                        return new Double(Metric.AVAIL_UP);
                    } else if (cause instanceof InstanceNotFoundException) {
                        return new Double(Metric.AVAIL_DOWN);
                    }
                    cause = cause.getCause();
                }
            }
            throw e;
        }
    }

    private static void removeMBeanConnector(Properties config) {
        String jmxUrl = config.getProperty(MxUtil.PROP_JMX_URL);
        if (cache.get(jmxUrl) != null) {
            log.debug("Removing (stale) cached connection for: " + jmxUrl);
            disconnect(jmxUrl);
        }
    }

    // vmid == pid; use undocumented ConnectorAddressLink.importFrom(pid)
    // to get the local JMXServiceURL
    static String getUrlFromPid(String ptql)
        throws IOException {
        Sigar sigar = new Sigar();
        String address;
        long pid;

        try {
            pid =
                        new ProcessFinder(sigar).findSingleProcess(ptql);

            Class caddrLinkClass =
                        Class.forName("sun.management.ConnectorAddressLink");

            Method importFrom =
                        caddrLinkClass.getMethod("importFrom",
                                    new Class[] { Integer.TYPE });

            address =
                        (String) importFrom.invoke(caddrLinkClass,
                                    new Object[] { new Integer((int) pid) });
        } catch (ClassNotFoundException e) {
            String jvm =
                        System.getProperty("java.vm.name") + " " +
                                    System.getProperty("java.vm.version");
            throw new IOException(ptql + " " + e.getMessage() +
                        " not supported by " + jvm);
        } catch (InvocationTargetException e) {
            throw new IOException(ptql + " " + e.getCause());
        } catch (Exception e) {
            throw new IOException(ptql + " " + e);
        } finally {
            sigar.close();
        }

        if (address == null) {
            throw new IOException("Unable to determine " +
                        PROP_JMX_URL + " using vmid=" + pid +
                        ".  Server must be started with: " +
                        "-Dcom.sun.management.jmxremote");
        }
        log.debug(PTQL_PREFIX + ptql + " resolved to vmid=" + pid +
                    ", " + PROP_JMX_URL + "=" + address);

        return address;
    }

    private static final Map<JMXConnectorKey, JMXConnector> mbeanConns = new HashMap<JMXConnectorKey, JMXConnector>();

    public static JMXConnector getCachedMBeanConnector(Properties config)
        throws MalformedURLException, IOException {
        String jmxUrl = config.getProperty(MxUtil.PROP_JMX_URL);
        String user = config.getProperty(PROP_JMX_USERNAME);
        String pass = config.getProperty(PROP_JMX_PASSWORD);
        JMXConnectorKey key = new JMXConnectorKey(jmxUrl, user, pass);
        JMXConnector rtn = null;
        synchronized (mbeanConns) {
            rtn = mbeanConns.get(key);
            if (rtn == null) {
                rtn = getMBeanConnector(config);
                mbeanConns.put(key, rtn);
            }
            try {
                // ensure that the connection is not broken
                rtn.getMBeanServerConnection();
            } catch (IOException e) {
                close(rtn);
                rtn = getMBeanConnector(config);
                mbeanConns.put(key, rtn);
            }
        }
        final JMXConnector c = rtn;
        final InvocationHandler handler = new InvocationHandler() {
            private final JMXConnector conn = c;

            public Object invoke(Object proxy,
                                 Method method,
                                 Object[] args)
                throws Throwable {
                if (method.getName().equals("close")) {
                    return null;
                }
                synchronized (conn) {
                    return method.invoke(conn, args);
                }
            }
        };
        return (JMXConnector) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[] { JMXConnector.class }, handler);
    }

    private static class JMXConnectorKey {
        private final String url;
        private final String user;
        private final String pass;

        private JMXConnectorKey(String url,
                                String user,
                                String pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }

        public boolean equals(Object rhs) {
            if (this == rhs) {
                return true;
            }
            if (!(rhs instanceof JMXConnectorKey)) {
                return false;
            }
            JMXConnectorKey r = (JMXConnectorKey) rhs;
            return r.url.equals(url) && equals(r.user, user) && equals(r.pass, pass);
        }

        private boolean equals(String buf1,
                               String buf2) {
            if (buf1 == buf2) {
                return true;
            }
            if (buf1 == null && buf2 != null || buf2 == null && buf1 != null) {
                return false;
            }
            return buf1.equals(buf2);
        }

        public int hashCode() {
            int rtn = url.hashCode() * 7;
            rtn += (user != null) ? user.hashCode() * 7 : 0;
            rtn += (pass != null) ? pass.hashCode() * 7 : 0;
            return rtn;
        }
    }

    public static JMXConnector getMBeanConnector(Properties config)
        throws MalformedURLException, IOException {

        String jmxUrl = config.getProperty(MxUtil.PROP_JMX_URL);
        Map map = new HashMap();

        String user = config.getProperty(PROP_JMX_USERNAME);
        String pass = config.getProperty(PROP_JMX_PASSWORD);

        map.put(JMXConnector.CREDENTIALS, new String[] { user, pass });

        // required for Oracle AS
        String providerPackages = config.getProperty(PROP_JMX_PROVIDER_PKGS);
        if (providerPackages != null)
            map.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, providerPackages);

        if (jmxUrl == null) {
            throw new MalformedURLException(PROP_JMX_URL + "==null");
        }

        if (jmxUrl.startsWith(PTQL_PREFIX)) {
            jmxUrl =
                        getUrlFromPid(jmxUrl.substring(PTQL_PREFIX.length()));
        }

        JMXServiceURL url = new JMXServiceURL(jmxUrl);

        String proto = url.getProtocol();
        if (proto.equals("t3") || proto.equals("t3s")) {
            // http://edocs.bea.com/wls/docs92/jmx/accessWLS.html
            // WebLogic support, requires:
            // cp $WLS_HOME/server/lib/wljmxclient.jar pdk/lib/
            // cp $WLS_HOME/server/lib/wlclient.jar pdk/lib/
            map.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,
                        "weblogic.management.remote");
            map.put(Context.SECURITY_PRINCIPAL, user);
            map.put(Context.SECURITY_CREDENTIALS, pass);
        }

        JMXConnector connector = JMXConnectorFactory.connect(url, map);
        if (log.isDebugEnabled()) {
            log.debug("created new JMXConnector url=" + url +
                        ", classloader=" + Thread.currentThread().getContextClassLoader());
        }
        return connector;
    }

    private static void disconnect(String jmxUrl) {
        Object obj = cache.remove(jmxUrl);
        if (obj != null) {
            JMXConnector connector = ((ConnectorInstance) obj).connector;
            close(connector);
            if (log.isDebugEnabled()) {
                log.debug("Closed previous connector (" + address(connector) + ") for: " + jmxUrl);
            }
        }
    }

    private static JMXConnector connect(Properties config,
                                        String jmxUrl,
                                        String jmxUsername,
                                        String jmxPassword)
        throws IOException {

        disconnect(jmxUrl);
        JMXConnector connector = getMBeanConnector(config);
        cache.put(jmxUrl, new ConnectorInstance(connector, jmxUsername, jmxPassword));
        log.debug("Opened new connector (" +
                    address(connector) + ") for: " + jmxUrl);
        return connector;
    }

    private static String address(Object obj) {
        return "@" + Integer.toHexString(obj.hashCode());
    }

    private static String mask(String val) {
        return val.replaceAll(".", "*");
    }

    private static String diff(String old,
                               String cur) {
        return "'" + old + "'->'" + cur + "'";
    }

    public static MBeanServerConnection getMBeanServer(Properties config)
        throws MalformedURLException,
        IOException {

        String jmxUrl = config.getProperty(MxUtil.PROP_JMX_URL);
        String jmxUsername = config.getProperty(MxUtil.PROP_JMX_USERNAME, "");
        String jmxPassword = config.getProperty(MxUtil.PROP_JMX_PASSWORD, "");

        boolean isCached = false;
        JMXConnector connector;
        ConnectorInstance instance = (ConnectorInstance) cache.get(jmxUrl);
        if (instance != null) {
            connector = instance.connector;
            String username = instance.username;
            String password = instance.password;
            boolean usernameChanged = !username.equals(jmxUsername);
            boolean passwordChanged = !password.equals(jmxPassword);
            if (usernameChanged || passwordChanged) {
                if (log.isDebugEnabled()) {
                    log.debug("Credentials changed. Reconnecting cached connection for: " + jmxUrl);
                }
                connector = connect(config, jmxUrl, jmxUsername, jmxPassword);
            }
            else {
                isCached = true;
            }
        } else {
            connector = connect(config, jmxUrl, jmxUsername, jmxPassword);
            log.debug("Caching connector for: " + jmxUrl);
        }

        try {
            return connector.getMBeanServerConnection();
        } catch (IOException e) {
            if (isCached) {
                log.debug("Reconnecting cached connection for: " + jmxUrl);
                connector = connect(config, jmxUrl, jmxUsername, jmxPassword);
                return connector.getMBeanServerConnection();
            }
            else {
                throw e;
            }
        }
    }

    public static Object getValue(Properties config,
                                  String objectName,
                                  String attribute)
        throws MalformedURLException,
        MalformedObjectNameException,
        IOException,
        MBeanException,
        AttributeNotFoundException,
        InstanceNotFoundException,
        ReflectionException,
        PluginException {
        ObjectName objName = new ObjectName(objectName);
        JMXConnector connector = null;
        try {
            connector = getCachedMBeanConnector(config);
            if (attribute.startsWith(STATS_PREFIX)) {
                return getJSR77Statistic(connector.getMBeanServerConnection(), objName, attribute);
            }
            else if (attribute.startsWith(COMPOSITE_PREFIX)) {
                return getCompositeMetric(connector.getMBeanServerConnection(), objName, attribute);
            }
            else {
                return connector.getMBeanServerConnection().getAttribute(objName, attribute);
            }
        } finally {
            close(connector);
        }
    }

    private static Object setAttribute(MBeanServerConnection mServer,
                                       ObjectName obj,
                                       String name,
                                       Object value)
        throws MetricUnreachableException,
        MetricNotFoundException,
        PluginException,
        ReflectionException,
        InstanceNotFoundException,
        MBeanException,
        IOException {

        if (name.startsWith("set")) {
            name = name.substring(3);
        }

        Attribute attr = new Attribute(name, value);

        try {
            mServer.setAttribute(obj, attr);
        } catch (AttributeNotFoundException e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        } catch (InvalidAttributeValueException e) {
            throw new ReflectionException(e);
        }

        return null;
    }

    private static Object getAttribute(MBeanServerConnection mServer,
                                       ObjectName obj,
                                       String name)
        throws MetricUnreachableException,
        MetricNotFoundException,
        PluginException,
        ReflectionException,
        InstanceNotFoundException,
        MBeanException,
        IOException {

        if (name.startsWith("get")) {
            name = name.substring(3);
        }

        try {
            return mServer.getAttribute(obj, name);
        } catch (AttributeNotFoundException e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        }
    }

    public static Object invoke(Properties config,
                                String objectName,
                                String method,
                                Object[] args,
                                String[] sig)
        throws MetricUnreachableException,
        MetricNotFoundException,
        PluginException {

        JMXConnector connector = null;

        try {
            connector = getMBeanConnector(config);
            MBeanServerConnection mServer =
                        connector.getMBeanServerConnection();
            ObjectName obj = new ObjectName(objectName);
            MBeanInfo info = mServer.getMBeanInfo(obj);

            if (sig.length == 0) {
                MBeanUtil.OperationParams params =
                            MBeanUtil.getOperationParams(info, method, args);
                if (params.isAttribute) {
                    if (method.startsWith("set")) {
                        return setAttribute(mServer, obj, method, params.arguments[0]);
                    } else {
                        return getAttribute(mServer, obj, method);
                    }
                }
                sig = params.signature;
                args = params.arguments;
            }

            return mServer.invoke(obj, method, args, sig);
        } catch (RemoteException e) {
            throw unreachable(config, e);
        } catch (MalformedObjectNameException e) {
            throw invalidObjectName(objectName, e);
        } catch (InstanceNotFoundException e) {
            throw objectNotFound(objectName, e);
        } catch (ReflectionException e) {
            throw error(objectName, e, method);
        } catch (IntrospectionException e) {
            throw error(objectName, e, method);
        } catch (MBeanException e) {
            throw error(objectName, e, method);
        } catch (IOException e) {
            throw error(objectName, e, method);
        } finally {
            close(connector, objectName, method);
        }
    }

    public static void close(JMXConnector connector) {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                log.error("error closing connector: " + e, e);
            }
        }
    }

    public static void close(JMXConnector connector,
                             String objectName,
                             String method) {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                log.error("error closing connector " + e +
                            ".  objectName=" + objectName + "method=" + method, e);
            }
        }
    }

    private static class ConnectorInstance {
        JMXConnector connector;
        String username;
        String password;

        ConnectorInstance(JMXConnector connector,
                          String username,
                          String password) {
            this.connector = connector;
            this.username = username;
            this.password = password;
        }
    }
}
