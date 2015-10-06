/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.security.SecurityUtil;

public abstract class Collector implements Runnable {

    public static final String PROP_HOSTNAME = "hostname";

    public static final String PROP_PORT = "port";

    public static final String PROP_PROTOCOL = "protocol";

    public static final String PROP_PATH = "path";

    public static final String PROP_SSL = "ssl";

    public static final String PROP_SSL_PROTOCOL = "sslprotocol";

    public static final String PROP_USERNAME = "user";

    public static final String PROP_PASSWORD = "pass";

    public static final String PROP_REALM = "realm";

    public static final String PROP_FOLLOW = "follow";

    public static final String PROP_METHOD = "method";

    public static final String PROP_SSLPORT =
                PROP_SSL + PROP_PORT;

    public static final String METHOD_HEAD = "HEAD";

    public static final String METHOD_GET = "GET";

    public static final String PROTOCOL_HTTP = "http";

    public static final String PROTOCOL_HTTPS = "https";

    public static final String PROTOCOL_FTP = "ftp";

    public static final String PROTOCOL_SOCKET = "socket";

    public static final String DEFAULT_HOSTNAME = "localhost";

    public static final String DEFAULT_FTP_PORT = "21";

    public static final String DEFAULT_HTTP_PORT = "80";

    public static final String DEFAULT_HTTPS_PORT = "443";

    public static final String PROP_TIMEOUT = "timeout";

    public static final String ATTR_RESPONSE_TIME = "ResponseTime";

    public static final String ATTR_RESPONSE_CODE = "ResponseCode";

    public static final String LISTEN_PORTS = "listen.ports";

    public static final String GUID = "GUID";

    public static final String MAC = "Mac";

    static Log log =
                LogFactory.getLog(Collector.class.getName());

    static Map containers = new HashMap();

    private GenericPlugin plugin;
    private Properties props;

    private boolean isRunning = false;
    private int timeout = -1;
    private long startTime, endTime;
    // use a ref to Metric: ScheduleThread.unscheduleMetric unsets interval
    private Metric intervalMetric;
    private long lastCollection = -1;
    private static Map compatAliases = new HashMap();

    static {
        // maintain compat w/ old templates
        String[][] aliases = {
                    // NetworkServer IP
                    { "RequestTime", ATTR_RESPONSE_TIME },
                    // Script, Nagios Plugin
                    { "ExecTime", ATTR_RESPONSE_TIME },
                    { "ReturnCode", ATTR_RESPONSE_CODE },
                    { "Arg", ExecutableProcess.PROP_FILE },
                    { "prefix", ExecutableProcess.PROP_EXEC },
                    { "Params", ExecutableProcess.PROP_ARGS },
                    // other
                    { "availability", Metric.ATTR_AVAIL },
        };
        for (int i = 0; i < aliases.length; i++) {
            compatAliases.put(aliases[i][0], aliases[i][1]);
        }
    }

    CollectorResult result = new CollectorResult();

    protected void init()
        throws PluginException {
        setSource(getPlugin().getName());
    }

    public abstract void collect();

    /**
     * Initialize a Collector instance for use outside of MeasurementPlugin. Collectors are generally used for metric
     * collection, but can also be used in some cases for inventory property discovery and/or control.
     * 
     * @param plugin A ServerDetector or ControlPlugin
     * @param config Resource configuration properties
     * @throws PluginException
     */
    public void init(GenericPlugin plugin,
                     ConfigResponse config)
        throws PluginException {

        this.plugin = plugin;
        setProperties(config.toProperties());
        init();
    }

    /**
     * Initialize and collect values for use outside of MeasurementPlugin. This method is useful for inventory property
     * discovery.
     * 
     * @param plugin A ServerDetector or ControlPlugin
     * @param config
     * @return Resource configuration properties
     * @throws PluginException
     */
    public Map getValues(GenericPlugin plugin,
                         ConfigResponse config)
        throws PluginException {

        init(plugin, config);
        collect();
        return getResult().values;
    }

    public int getTimeout() {
        if (this.timeout == -1) {
            this.timeout =
                        getIntegerProperty(getPropTimeout(),
                                    getDefaultTimeout());
        }
        return this.timeout;
    }

    public int getTimeoutMillis() {
        return getTimeout() * 1000;
    }

    protected int getDefaultTimeout() {
        return 30; // 30 seconds
    }

    protected String getPropTimeout() {
        return PROP_TIMEOUT;
    }

    protected int getIntegerProperty(String key,
                                     int defVal) {
        String val = getProperties().getProperty(key);
        if (val == null) {
            if (defVal == -1) {
                String msg = "Missing " + key + " property";
                throw new IllegalArgumentException(msg);
            }
            return defVal;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            String msg =
                        "Invalid value (" + val + ")" +
                                    " for " + key;
            throw new IllegalArgumentException(msg);
        }
    }

    protected String getCollectorProperty(String key,
                                          String defVal) {
        return this.props.getProperty(key, defVal);
    }

    static Object getCompatValue(Map map,
                                 String key) {
        Object val = map.get(key);
        if (val == null) {
            Object alias = compatAliases.get(key);
            if (alias == null) {
                alias = key.toLowerCase();
            }
            if (alias != null) {
                val = map.get(alias);
            }
        }
        return val;
    }

    protected String getCollectorProperty(String key) {
        return (String) getCompatValue(this.props, key);
    }

    protected void setProperties(Properties props) {
        this.props = props;
    }

    protected Properties getProperties() {
        return this.props;
    }

    protected GenericPlugin getPlugin() {
        return this.plugin;
    }

    protected void setSource(String value) {
        this.result.source = value;
    }

    protected String getSource() {
        return this.result.source;
    }

    protected void setLogLevel(int value) {
        this.result.level = value;
    }

    protected int getLogLevel() {
        return this.result.level;
    }

    protected void setMessage(String value) {
        this.result.message = value;
    }

    protected String getMessage() {
        return this.result.message;
    }

    private String composeMessage(String msg,
                                  Throwable t) {
        return msg + ": " + t.getMessage();
    }

    protected void setMessage(String msg,
                              Throwable t) {
        setMessage(composeMessage(msg, t));
    }

    protected void setErrorMessage(String msg) {
        setLogLevel(LogTrackPlugin.LOGLEVEL_ERROR);
        setMessage(msg);
    }

    protected void setWarningMessage(String msg) {
        setLogLevel(LogTrackPlugin.LOGLEVEL_WARN);
        setMessage(msg);
    }

    protected void setInfoMessage(String msg) {
        setLogLevel(LogTrackPlugin.LOGLEVEL_INFO);
        setMessage(msg);
    }

    protected void setDebugMessage(String msg) {
        setLogLevel(LogTrackPlugin.LOGLEVEL_DEBUG);
        setMessage(msg);
    }

    protected void setErrorMessage(String msg,
                                   Throwable t) {
        setErrorMessage(composeMessage(msg, t));
    }

    protected void setWarningMessage(String msg,
                                     Throwable t) {
        setWarningMessage(composeMessage(msg, t));
    }

    protected void setInfoMessage(String msg,
                                  Throwable t) {
        setInfoMessage(composeMessage(msg, t));
    }

    protected void setDebugMessage(String msg,
                                   Throwable t) {
        setDebugMessage(composeMessage(msg, t));
    }

    protected void setValue(String key,
                            String val) {
        this.result.setValue(key, val);
    }

    protected void addValues(Map values) {
        this.result.addValues(values);
    }

    protected CollectorResult getResult() {
        return this.result;
    }

    protected void setValue(String key,
                            double val) {
        this.result.setValue(key, val);
    }

    protected void setAvailability(double val) {
        setValue(Metric.ATTR_AVAIL, val);
    }

    protected void setAvailability(boolean val) {
        setAvailability(val ?
                    Metric.AVAIL_UP : Metric.AVAIL_DOWN);
    }

    protected void setResponseCode(int code) {
        setValue(ATTR_RESPONSE_CODE, code);
    }

    protected void setResponseTime(double value) {
        setValue(ATTR_RESPONSE_TIME, value);
    }

    protected void startTime() {
        this.startTime = System.currentTimeMillis();
    }

    protected void endTime() {
        this.endTime = System.currentTimeMillis();
    }

    String mapToString(Map map) {
        Map props = new HashMap();
        for (Iterator it = map.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            String secured = (ConfigSchema.isSecret(key) || ConfigSchema.isSensitive(key)) ?
                        SecurityUtil.HIDDEN_VALUE_MASK : val;

            props.put(key, secured);
        }
        return props.toString();
    }

    @Override
    public String toString() {
        return mapToString(this.props);
    }

    static class PluginContainer {
        String name;

        Map<CollectorIdentifier, Collector> collectors =
                    Collections.synchronizedMap(new HashMap<CollectorIdentifier, Collector>());
        Map<CollectorIdentifier, CollectorResult> results =
                    Collections.synchronizedMap(new HashMap<CollectorIdentifier, CollectorResult>());

        static PluginContainer get(GenericPlugin plugin) {
            String name = plugin.getName();
            synchronized (containers) {
                PluginContainer container =
                            (PluginContainer) containers.get(name);
                if (container == null) {
                    container = new PluginContainer();
                    container.name = name;
                    containers.put(name, container);
                }
                return container;
            }
        }

        static void setResult(Collector collector) {
            CollectorResult result = new CollectorResult(collector);
            if (log.isDebugEnabled()) {
                log.debug("name=" + collector.plugin.getName() + ", " +
                            "thread=" + Thread.currentThread().getName() +
                            ", result=" + result);
            }
            get(collector.plugin).results.put(new CollectorIdentifier(collector.props), result);
        }
    }

    public MetricValue getValue(Metric metric,
                                CollectorResult result) {
        return result.getMetricValue(metric.getAttributeName());
    }

    private long getInterval() {
        if (this.intervalMetric == null) {
            return -1;
        }
        else {
            return this.intervalMetric.getInterval();
        }
    }

    private void setInterval(Metric metric) {
        this.intervalMetric = metric;
    }

    // interval is used to make collection to happen 1 minute before
    // the Availability metric is scheduled to be collected
    protected void setInterval(MeasurementPlugin plugin,
                               Metric metric) {
        if (!isPoolable()) {
            return; // XXX apply only to netservices and exec for now.
        }
        long itv = metric.getInterval();
        boolean isAvail =
                    metric.getAttributeName().equals(Metric.ATTR_AVAIL);

        if ((isAvail || (getInterval() == -1)) &&
                    (itv > 0) &&
                    (getInterval() != itv))
        {
            setInterval(metric);
            if (log.isDebugEnabled()) {
                log.debug("Set itv=" + (getInterval() / MINUTE) +
                            "min for " + plugin.getName() +
                            " collector: " + this);
            }
        }
    }

    public static MetricValue getValue(MeasurementPlugin plugin,
                                       Metric metric)
        throws PluginException,
        MetricNotFoundException,
        MetricUnreachableException {

        CollectorResult result;
        Collector collector;
        Properties props =
                    plugin.getCollectorProperties(metric);

        PluginContainer container =
                    PluginContainer.get(plugin);
        CollectorIdentifier collectorIdentifier = new CollectorIdentifier(props);
        collector = (Collector) container.collectors.get(collectorIdentifier);
        result = (CollectorResult) container.results.get(collectorIdentifier);

        if (result != null) {
            boolean isAvail =
                        metric.getAttributeName().equals(Metric.ATTR_AVAIL);
            result.collected = true;
            if (!result.reported && (result.level != -1)) {
                plugin.getManager().reportEvent(metric,
                            result.getTimeStamp(),
                            result.getLevel(),
                            result.getSource(),
                            result.getMessage());
                result.reported = true;
            }

            MetricValue value;

            boolean setClassLoader =
                        PluginLoader.setClassLoader(collector);

            try {
                value = collector.getValue(metric, result);
                if (isAvail) {
                    collector.lastCollection = value.getTimestamp();
                }
            } finally {
                collector.setInterval(plugin, metric); // sync
                if (setClassLoader) {
                    PluginLoader.resetClassLoader(collector);
                }
            }

            if (value == null) {
                throw new MetricNotFoundException(metric.toString());
            }

            return value;
        }

        if (collector == null) {
            collector = plugin.getNewCollector();
            collector.plugin = plugin;

            boolean setClassLoader =
                        PluginLoader.setClassLoader(collector);

            try {
                collector.setProperties(props);
                collector.init();
                collector.setInterval(plugin, metric);
            } finally {
                if (setClassLoader) {
                    PluginLoader.resetClassLoader(collector);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Adding " + plugin.getName() +
                            " collector: " + collector);
            }

            container.collectors.put(collectorIdentifier, collector);
        }

        // just added collector to the thread,
        // next time will pickup metrics.
        return MetricValue.FUTURE;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Collector)) {
            return false;
        }

        return ((Collector) obj).props.equals(this.props);
    }

    @Override
    public int hashCode() {
        return this.props.hashCode();
    }

    public boolean isPoolable() {
        return false;
    }

    public void run() {
        this.isRunning = true;
        this.result.values.clear();
        this.result.level = -1;
        this.startTime = this.endTime = -1;

        boolean setClassLoader =
                    PluginLoader.setClassLoader(this);

        try {
            collect();
        } catch (Throwable t) {
            log.error("Error running " + this.plugin.getName() +
                        " collector: " + t, t);
            setErrorMessage("Error: " + t.getMessage(), t);
            setAvailability(false);
        } finally {
            if (setClassLoader) {
                PluginLoader.resetClassLoader(this);
            }
        }

        if (this.endTime != -1) {
            this.result.timestamp = this.endTime;
            setResponseTime(this.endTime - this.startTime);
        }
        else {
            this.result.timestamp = System.currentTimeMillis();
        }

        PluginContainer.setResult(this);
        this.isRunning = false;
    }

    protected void parseResults(String message) {
        boolean hasResultValue = false;
        StringTokenizer st = new StringTokenizer(message, "\r\n,");

        while (st.hasMoreTokens()) {
            String attr = st.nextToken();

            int ix = attr.indexOf('=');

            if (ix == -1) {
                if (!hasResultValue) {
                    StringTokenizer st2 =
                                new StringTokenizer(attr, " \t");
                    while (st2.hasMoreTokens()) {
                        String s = st2.nextToken();

                        if (Character.isDigit(s.charAt(0)) &&
                                    Character.isDigit(s.charAt(s.length() - 1)))
                        {
                            setValue("ResultValue", s);
                            hasResultValue = true; // use the first number
                        }
                    }
                }
                continue;
            }
            String key = attr.substring(0, ix);
            String val = attr.substring(key.length() + 1);

            if (key.equals("Message")) {
                setMessage(val);
                continue;
            }

            setValue(key, val);
        }
    }

    private static final long SECOND = 1 * 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    public static final String REMOVABLE = "removable";
    public static final String ALLOW_REMOVE = "allow_remove";

    private static String lastRun(long now,
                                  long time) {
        long delta = now - time;

        if ((delta / MINUTE) < 1) {
            long seconds = delta / SECOND;
            return seconds + " seconds ago";
        }

        if ((delta / HOUR) < 1) {
            long minutes = delta / MINUTE;
            return minutes + " minutes ago";
        }

        if ((delta / DAY) < 1) {
            long hours = delta / HOUR;
            return hours + " hours ago";
        }

        return "on " + new Date(time);
    }

    private static Collection<Collector> getCollectorsToExecute(PluginContainer container) {
        final boolean debug = log.isDebugEnabled();
        final Collection<Collector> rtn = new ArrayList<Collector>();
        if (debug)
            log.debug("Running " + container.name + " collectors");
        List pluginCollectors;
        // copy so we don't block PluginCollector.get()
        synchronized (container.collectors) {
            Collection values = container.collectors.values();
            pluginCollectors = new ArrayList(values.size());
            pluginCollectors.addAll(values);
        }
        for (int i = 0; i < pluginCollectors.size(); i++) {
            Collector collector = (Collector) pluginCollectors.get(i);
            long interval = collector.getInterval();
            long lastCollection = collector.lastCollection;
            if (collector.isRunning) {
                if (debug)
                    log.debug(collector + " is running: deferring");
                continue;
            }
            CollectorResult result = container.results.get(new CollectorIdentifier(collector.props));
            if ((result != null) && (result.values.size() != 0)) {
                long now = System.currentTimeMillis();
                boolean shouldSkip = true;
                if ((interval != -1) && (lastCollection != -1)) {
                    if ((now - lastCollection) >= interval - MINUTE) {
                        // ScheduleThread should be picking this up
                        // within the next minute, so collect now.
                        shouldSkip = false;
                    } else if ((now - result.timestamp) >= interval) {
                        // not in sync w/ ScheduleThread
                        shouldSkip = false;
                    }
                } else {
                    shouldSkip = !result.collected;
                }
                String msg = null;
                if (debug) {
                    String itv, coll;
                    if (interval == -1) {
                        itv = "unknown";
                    } else {
                        itv = (interval / MINUTE) + "min";
                    }
                    if (lastCollection == -1) {
                        coll = "n/a";
                    } else {
                        coll = lastRun(now, lastCollection);
                    }
                    msg = collector +
                                " ran " + lastRun(now, result.timestamp) + "," +
                                " consumed " + coll + ", " + itv + " itv: ";
                }
                if (shouldSkip) {
                    if (debug)
                        log.debug(msg + "deferring.");
                    continue;
                } else {
                    if (debug)
                        log.debug(msg + "collecting.");
                }
            }
            rtn.add(collector);
        }
        return rtn;
    }

    public static Collection<Collector> getCollectorsToExecute() {
        Collection<Collector> rtn = new ArrayList<Collector>();
        List pluginContainers;
        // copy so we don't block PluginCollector.get()
        synchronized (containers) {
            Collection values = containers.values();
            pluginContainers = new ArrayList(values.size());
            pluginContainers.addAll(values);
        }
        for (int i = 0; i < pluginContainers.size(); i++) {
            PluginContainer container = (PluginContainer) pluginContainers.get(i);
            rtn.addAll(getCollectorsToExecute(container));
        }
        return rtn;
    }

    public static void refreshOnPluginsChange() {
        synchronized (containers) {
            containers.clear();
        }
    }

    public static void main(String[] args)
        throws Exception {
        Collector collector = new ExecutableProcess();
        collector.setProperties(System.getProperties());
        collector.init();
        collector.run();
        System.out.println(collector);
    }
}
