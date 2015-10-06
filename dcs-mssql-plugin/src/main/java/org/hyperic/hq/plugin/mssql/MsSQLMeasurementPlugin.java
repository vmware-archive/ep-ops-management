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
package org.hyperic.hq.plugin.mssql;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.pdh.MultipleCollectQueryPdh;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLMeasurementPlugin extends MeasurementPlugin {

    private static Log log = LogFactory.getLog(MsSQLMeasurementPlugin.class);
    static final String DEFAULT_SQLSERVER_METRIC_PREFIX = "SQLServer";
    static final String DEFAULT_SQLAGENT_METRIC_PREFIX = "SQLAgent";

    @Override
    public String translate(String template, ConfigResponse config) {
        template = super.translate(template, config);
        MsSQLDetector.debug(log, "[translate] > template = " + template);
        if (template.contains(":collector:")) {
            int lastSemiColon = template.lastIndexOf(':');
            template = template.substring(0, lastSemiColon) + ',' + template.substring(lastSemiColon + 1);
        }
        MsSQLDetector.debug(log, "[translate] < template = " + template);
        return template;
    }

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MsSQLDetector.debug(log, "[getValue] metric: " + metric);

        Boolean isOwner = isOwnerOfCluster(metric); //Will return true if this is a standalone resource, or an owner of a cluster
        if(!isOwner) {
            return MetricValue.NONE;
        }

        if (metric.getDomainName().equalsIgnoreCase("collector")) {
            MsSQLDetector.debug(log, "[getValue] collectorProperties: " + getCollectorProperties(metric));
            return Collector.getValue(this, metric);
        } else if (metric.getDomainName().equalsIgnoreCase("query")) {
            if (metric.getAttributeName().equalsIgnoreCase("alloc")) {
                return getAllocFromFile(metric);
            } else if (metric.getAttributeName().equalsIgnoreCase("max_size")) {
                return getMaxSizeFromFile(metric);
            } else if (metric.getAttributeName().equalsIgnoreCase("uptime")) {
                return getUpTime(metric);
            } else if (metric.getAttributeName().equalsIgnoreCase("recovery_model")) {
                return getRecoveryModel(metric);
            }
        } else if (metric.getDomainName().equalsIgnoreCase("pdh")) {
            return getPDHMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdh2")) {
            return getPDHInstaceMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdhDBAvail")) {
            return getPDHDBAvailMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("service")) {
            return checkServiceAvail(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("mssql")) {
            if (metric.getObjectPropString().equals("process")) {
                return getInstanceProcessMetric(metric);
            }
        } else if (metric.getDomainName().equalsIgnoreCase("dfp")) {
            return Collector.getValue(this, metric);
        }

        MsSQLDetector.debug(log, "[getValue] Unable to retrieve value for metric: " + metric);
        return MetricValue.NONE;
    }

    private Boolean isOwnerOfCluster(Metric metric){
        String instanceName = getInstanceName(metric);

        if(ClusterDetect.isCluster()) {
            if (metric.isAvail()) {
                // We assume that availibity metric is schdelued for one minute. We use this, to update this instance name
                // status. If it becomes active node, we will measure all of its metrics. If it became passive, then we
                // won't.
                ClusterDetect.pollInstanceOwner(instanceName);
            }

            if (!Boolean.TRUE.equals(ClusterDetect.isOwner(instanceName))) {
                log.debug(String.format(
                        "Cannot measure metrics for instance %s - this server is not the owner of the instance",
                        instanceName));
                return false;
            }
        }
        return true;
    }

    private String getInstanceName(Metric metric) {
        String id = metric.getId();
        String serviceName;
        if (id.startsWith("2")) { // if this is a server resource
            serviceName = metric.getProperties().getProperty("service_name");
        } else {
            serviceName = metric.getProperties().getProperty("instance");
        }
        return MsSQLDetector.extractInstanceName(serviceName);
    }

    @Override
    public Collector getNewCollector() {
        if (!getPluginData().getPlugin("collector", "MSSQL Database").equals(MsSQLDataBaseCollector.class.getName())) {
            getPluginData().addPlugin("collector", "MSSQL Database", MsSQLDataBaseCollector.class.getName());
        }
        Collector c = super.getNewCollector();
        getLog().debug("[getNewCollector] t:'" + getTypeInfo().getName() + "' c:" + c.getClass().getName());
        return c;
    }

    private MetricValue getInstanceProcessMetric(Metric metric) {
        try {
            MsSQLDetector.debug(log, "[gipm] metric='" + metric + "'");
            String serviceName = metric.getProperties().getProperty("service_name");
            Sigar sigar = new Sigar();
            long servicePID = sigar.getServicePid(serviceName);
            MsSQLDetector.debug(log, "[gipm] serviceName='" + serviceName + "' servicePID='" + servicePID + "'");

            List<String> instances = Arrays.asList(MultipleCollectQueryPdh.getInstances("Process"));
            String serviceInstance = null;
            for (int i = 0; (i < instances.size()) && (serviceInstance == null); i++) {
                String instance = instances.get(i);
                if (instance.startsWith("sqlservr")) {
                    String obj = "\\Process(" + instance + ")\\ID Process";
                    MsSQLDetector.debug(log, "[gipm] obj='" + obj + "'");
                    double pid = MultipleCollectQueryPdh.getValue(obj);
                    if (pid == servicePID) {
                        serviceInstance = instance;
                        MsSQLDetector.debug(log, "[gipm] serviceName='" + serviceName + "' serviceInstance='" + serviceInstance + "'");
                    }
                }
            }

            if (serviceInstance != null) {
                String obj = "\\Process(" + serviceInstance + ")\\" + metric.getAttributeName();
                MsSQLDetector.debug(log, "[gipm] obj = '" + obj + "'");

                double res = MultipleCollectQueryPdh.getValue(obj);
                MsSQLDetector.debug(log, "[getPDH] obj:'" + obj + "' val:'" + res + "'");

                return new MetricValue(res);
            } else {
                MsSQLDetector.debug(log, "[gipm] Process for serviceName='" + serviceName + "' not found, returning " + MetricValue.NONE.getValue());
                return MetricValue.NONE;
            }

        } catch (Exception ex) {
            MsSQLDetector.debug(log, "[gipm] " + ex, ex);
            return MetricValue.NONE;
        }
    }

    private MetricValue checkServiceAvail(Metric metric) throws MetricUnreachableException {
        String service = metric.getObjectProperty("service_name");
        MsSQLDetector.debug(log, "[checkServiceAvail] service='" + service + "'");
        double res = Metric.AVAIL_DOWN;
        try {
            if (service != null) {
                Service s = new Service(service);
                if (s.getStatus() == Service.SERVICE_RUNNING) {
                    res = Metric.AVAIL_UP;
                }
                MsSQLDetector.debug(log, "[checkServiceAvail] service='" + service + "' metric:'" + metric + "' res=" + res);
            }
        } catch (Win32Exception ex) {
            MsSQLDetector.debug(log, "[checkServiceAvail] error. service='" + service + "' metric:'" + metric + "'", ex);
        }

        if ((res == Metric.AVAIL_UP) && (metric.getObjectProperties().getProperty("testdbcon", "").equalsIgnoreCase("true"))) {
            List<String> dbsFileNamesCMD = MsSQLDataBaseCollector.prepareSqlCommand(metric.getObjectProperties());
            dbsFileNamesCMD.add("-Q");
            dbsFileNamesCMD.add("select physical_name from sys.master_files");
            List<List<String>> test;
            try {
                test = MsSQLDataBaseCollector.executeSqlCommand(dbsFileNamesCMD);
            } catch (PluginException ex) {
                MetricUnreachableException e = new MetricUnreachableException("Unable to connect to the DB, review the user/password/sqlserver_name/instance options. " + ex.getMessage(), ex);
                log.error(e, e);
                throw e;
            }
            if (test.size() == 0) {
                MetricUnreachableException e = new MetricUnreachableException("Unable to connect to the DB, review the user/password/sqlserver_name/instance options.");
                log.error(e, e);
                throw e;
            }
        }

        return new MetricValue(res);
    }

    private MetricValue getPDHDBAvailMetric(Metric metric) {
        String dbName = metric.getObjectProperty("db.name");
        String service = metric.getProperties().getProperty("service_name");
        if (MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME.equalsIgnoreCase(service)) {
            MsSQLDetector.debug(log, "[getPDHDBAvailMetric] service='" + service + "' ==> ='" + MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME + "''");
            service = DEFAULT_SQLSERVER_METRIC_PREFIX;
        }
        String obj = service + ":Databases";
        MsSQLDetector.debug(log, "[getPDHDBAvailMetric] dbName='" + dbName + "' service='" + service + "' obj='" + obj + "'");
        double res = Metric.AVAIL_DOWN;
        try {
            if (dbName != null) {
                List<String> instances = Arrays.asList(MultipleCollectQueryPdh.getInstances(obj));
                if (instances.contains(dbName)) {
                    res = Metric.AVAIL_UP;
                }
                MsSQLDetector.debug(log, "[getPDHDBAvailMetric] service='" + service + "' dbName:'" + dbName + "' res=" + res);
            }
        } catch (PluginException ex) {
            MsSQLDetector.debug(log, "[getPDHDBAvailMetric] error. service='" + service + "' dbName:'" + dbName + "'", ex);
        }
        return new MetricValue(res);
    }

    // get metrics from perfmon
    private MetricValue getPDHInstaceMetric(Metric metric) {
        String obj = "\\" + metric.getObjectPropString();
        obj += "\\" + metric.getAttributeName();

        Enumeration<Object> ks = metric.getProperties().keys();
        while (ks.hasMoreElements()) {
            String k = (String) ks.nextElement();
            String v = metric.getProperties().getProperty(k);
            obj = obj.replaceAll("%" + k + "%", v);
        }
        getPDH(obj, metric);
        return getPDH(obj, metric);
    }

    // get metrics from perfmon
    private MetricValue getPDHMetric(Metric metric) {
        String prefix = metric.getProperties().getProperty("pref_prefix");
        if (prefix == null) {
            prefix = metric.getProperties().getProperty("service_name");
        }

        if (MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME.equalsIgnoreCase(prefix)) {
            prefix = DEFAULT_SQLSERVER_METRIC_PREFIX;
        }

        String obj = "\\" + prefix + ":" + metric.getObjectPropString();

        if (!metric.isAvail()) {
            obj += "\\" + metric.getAttributeName();
        }

        return getPDH(obj, metric);
    }

    private MetricValue getPDH(String obj, Metric metric) {
        MetricValue res;
        try {
            obj = obj.replaceAll("\\%3A", ":");
            obj = obj.replaceAll("\\%\\%", "%");
            double val = MultipleCollectQueryPdh.getValue(obj);
            MsSQLDetector.debug(log, "[getPDH] obj:'" + obj + "' val:'" + val + "'");
            res = new MetricValue(val);
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_UP);
            }
        } catch (Exception ex) {
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN);
                MsSQLDetector.debug(log, "[getPDH] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage(), ex);
            } else {
                res = MetricValue.NONE;
                MsSQLDetector.debug(log, "[getPDH] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage());
            }
        }
        return res;
    }

    private MetricValue getAllocFromFile(Metric metric) throws PluginException {
        try {
            String file = metric.getObjectProperty("file");
            if (file != null) {
                file = file.replaceAll("\\%3A", ":");
                //Protect from command line injection & SQL injection
                if (!isValidPath(file)) {
                    throw new Exception("Invalid File Name");
                }

                List<String> dbsFileNamesCMD = MsSQLDataBaseCollector.prepareSqlCommand(metric.getObjectProperties());
                dbsFileNamesCMD.add("-Q");
                dbsFileNamesCMD.add("select (case is_percent_growth when 0 then growth*8 else (size*8)*growth/100 end) nextAllocKB from sys.master_files where physical_name='" + file + "'");
                List<List<String>> res = MsSQLDataBaseCollector.executeSqlCommand(dbsFileNamesCMD);

                for (List<String> line : res) {
                    return new MetricValue(Double.parseDouble(line.get(0)));
                }
            }
        } catch (Exception ex) {
            MsSQLDetector.debug(log, ex.toString(), ex);
        }
        return MetricValue.NONE;
    }

    private MetricValue getMaxSizeFromFile(Metric metric) throws PluginException {
        try {
            String file = metric.getObjectProperty("file");
            if (file != null) {
                file = file.replaceAll("\\%3A", ":");
                //Protect from command line injection & SQL injection
                if (!isValidPath(file)) {
                    throw new Exception("Invalid File Name");
                }
                List<String> dbsFileNamesCMD = MsSQLDataBaseCollector.prepareSqlCommand(metric.getObjectProperties());
                dbsFileNamesCMD.add("-Q");
                dbsFileNamesCMD.add("select (case max_size when -1 then -1 else (convert(decimal,max_size)*8) end) max_size  from sys.master_files where physical_name='" + file + "'");
                List<List<String>> res = MsSQLDataBaseCollector.executeSqlCommand(dbsFileNamesCMD);

                for (List<String> line : res) {
                    return new MetricValue(Double.parseDouble(line.get(0)));
                }
            }
        } catch (Exception ex) {
            MsSQLDetector.debug(log, ex.toString(), ex);
        }
        return MetricValue.NONE;
    }

    private MetricValue getUpTime(Metric metric) {
        try {
            List<String> cmd = MsSQLDataBaseCollector.prepareSqlCommand(metric.getObjectProperties());
            cmd.add("-Q");
            cmd.add("select DATEDIFF(ss,sqlserver_start_time,GETDATE()) from sys.dm_os_sys_info");
            List<List<String>> res = MsSQLDataBaseCollector.executeSqlCommand(cmd);
            for (List<String> line : res) {
                return new MetricValue(Double.parseDouble(line.get(0)));
            }
        } catch (Exception ex) {
            MsSQLDetector.debug(log, ex.toString(), ex);
        }
        return MetricValue.NONE;
    }

    private MetricValue getRecoveryModel(Metric metric) {
        try {
            String name = metric.getObjectProperty("db.name");

            if (name != null) {
                //Protect from command line injection & SQL injection
                if (!isValidDbName(name)) {
                    throw new Exception("Invalid db name");
                }

                List<String> cmd = MsSQLDataBaseCollector.prepareSqlCommand(metric.getObjectProperties());
                cmd.add("-Q");
                cmd.add("select recovery_model from sys.databases where name='" + name + "'");
                List<List<String>> res = MsSQLDataBaseCollector.executeSqlCommand(cmd);
                for (List<String> line : res) {
                    return new MetricValue(Double.parseDouble(line.get(0)));
                }
            }
        } catch (Exception ex) {
            MsSQLDetector.debug(log, ex.toString(), ex);
        }
        return MetricValue.NONE;
    }
    // white list for path/file name according to https://msdn.microsoft.com/en-us/library/windows/desktop/aa365247%28v=vs.85%29.aspx
    // specifically do not allow ",'
    private static final String drive = "\\p{L}:\\\\";
    private static final String fileName= "([\\p{L}\\p{N}\\x20-\\xFF&&[^\\\\/:*?\"'<>\\|\r\n]])+";
    private static final String PATH_REGEX = "^"+ drive + "(" + fileName + "\\\\)*" + fileName +"$"; //white list

    // white list for mssql db name according to https://msdn.microsoft.com/en-us/library/ms175874.aspx
    private static final String DBNAME_REGEX = "^([\\p{L}\\p{N}@$#_&\\x20])+$";

    private boolean isValidPath(String str) {
        return isValid(str, PATH_REGEX);
    }

    private boolean isValidDbName(String str) {
        return isValid(str, DBNAME_REGEX);
    }

    private boolean isValid(String str, String regex) {

        boolean isMatched = Pattern.matches(regex,str);

        if (isMatched) {
            return true;
        }
        return false;
    }
}
