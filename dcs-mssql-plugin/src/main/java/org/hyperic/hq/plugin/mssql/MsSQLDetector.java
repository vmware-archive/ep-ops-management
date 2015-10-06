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

package org.hyperic.hq.plugin.mssql;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLDetector extends ServerDetector implements AutoServerDetector {

    private static final String ACTIVE = "active";
    private static final String REPORTING_SERVICES = "Reporting Services";
    static final String PROP_DB = "db.name";
    private static final String DB_NAME = "Database";
    static final String DEFAULT_SQLSERVER_SERVICE_NAME = "MSSQLSERVER";
    static final String DEFAULT_SQLAGENT_SERVICE_NAME = "SQLSERVERAGENT";
    static final String MS_CLUSTER_DISCOVERY = "MS_CLUSTER_DISCOVERY";
    static final String PROP_DETECTED_VERSION = "version";
    private static final String MSSQL_AGENT_NAME = "MSSQL Agent";
    public static final String PROP_SERVICENAME = "service_name";
    public static final String SERVER_NAME_SEPARATOR = "$";

    public static String PROP_CLUSTER_NAME = "mssql-cluster-name";
    public static String PROP_PLATFORM_NAME = "virtual-platform-name";
    public static String PROP_NETWORK_NAME = "sqlserver_name";
    public static String PROP_CLUSTER_NODES = "cluster-nodes";
    public static String PROP_ORIGINAL_PLATFORM = "original-platform-name";

    private static final Log log = LogFactory.getLog(MsSQLDetector.class);

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List cfgs;
        try {
            cfgs = Service.getServiceConfigs("sqlservr.exe");
        } catch (Win32Exception e) {
            debug(log,"[getServerResources] Error: " + e.getMessage(), e);
            return null;
        }

        debug(log,"[getServerResources] MSSQL Server found:'" + cfgs.size() + "'");

        if (cfgs.size() == 0) {
            return null;
        }
        List<ServerResource> servers = new ArrayList<ServerResource>();
        for (int i = 0; i < cfgs.size(); i++) {
            ServiceConfig serviceConfig = (ServiceConfig) cfgs.get(i);
            String name = serviceConfig.getName();
            Service mssqlService = null;
            boolean serverIsRunning = false;
            try {
                mssqlService = new Service(name);
                serverIsRunning = (mssqlService.getStatus() == Service.SERVICE_RUNNING);
            } catch (Win32Exception e) {
                debug(log,"[getServerResources] Error getting '" + name + "' service information " + e, e);
            } finally {
                if (mssqlService != null) {
                    mssqlService.close();
                }
            }
            File dir = new File(serviceConfig.getExe()).getParentFile();

            String detectedVersion = checkServerVersion(serviceConfig);

            if (detectedVersion != null) {
                dir = dir.getParentFile(); //strip "Binn"
                ServerResource server = createServerResource(dir.getAbsolutePath(), name, detectedVersion, serverIsRunning);
                if (server != null) {
                    servers.add(server);
                }
            }
        }
        return servers;
    }

    private String checkServerVersion(ServiceConfig serviceConfig) {
        String detectedVersion = null;
        String instance = instaceName(serviceConfig.getName());
        File dir = new File(serviceConfig.getExe()).getParentFile();

        detectedVersion = checkServerVersion(instance);

        if (detectedVersion == null) {
            detectedVersion = checkServerVersionOldStyle(dir);
        }

        return detectedVersion;
    }

    /**
     * Try to detect server version through Registry keys
     *
     * @param instance
     * @return
     */
    private String checkServerVersion(String instance) {
        String detectedVersion = null;
        boolean correctVersion = false;
        Map<String, String> regKeys = getTypeMultiProperty("regKey"); // Get all properties occurrence with their server
        // version

        if (regKeys.keySet().isEmpty()) {
            return null;
        }
        for (String regKey : regKeys.keySet()) {
            try {
                String suspectedVersion = regKeys.get(regKey);
                regKey = regKey.replace("%NAME%", instance);
                debug(log, "[getServerResources] regKey:'" + regKey + "'");
                RegistryKey key = RegistryKey.LocalMachine.openSubKey(regKey);
                String version = key.getStringValue("CurrentVersion");
                String expectedVersion = getPropertyByVersion("version", suspectedVersion);
                correctVersion = Pattern.compile(expectedVersion).matcher(version).find();
                debug(log, "[getServerResources] server:'" + instance + "' version:'" + version
                            + "' expectedVersion:'" + expectedVersion + "' correctVersion:'"
                            + correctVersion + "'");
                detectedVersion = suspectedVersion;
                break; // as version detected
            } catch (Win32Exception ex) {
                debug(log, "[getServerResources] Error accesing to windows registry to get '" + instance
                            + "' version. " + ex.getMessage());
            }
        }

        return detectedVersion;
    }

    /**
     * Try to detect server version through DLL file existence
     *
     * @param dir
     * @return
     */
    private String checkServerVersionOldStyle(File dir) {
        String detectedVersion = null;
        Map<String, String> versionFiles = getTypeMultiProperty("mssql.version.file");

        for (String versionFile : versionFiles.keySet()) {
            String suspectedVersion = versionFiles.get(versionFile);
            File dll = new File(dir, versionFile);
            boolean correctVersion = dll.exists();
            if (correctVersion) {
                getLog().debug("[checkVersionOldStyle] dll:'" + dll + "' correctVersion='" + correctVersion + "'");
                detectedVersion = suspectedVersion;
                break;
            }
        }
        return detectedVersion;
    }

    private ServerResource createServerResource(String installpath,
                                                String name,
                                                String detectedVersion,
                                                boolean serverIsRunning) {
        ServerResource server = createServerResource(installpath);
        ConfigResponse properties = new ConfigResponse();
        ConfigResponse cfg = new ConfigResponse();

        String instance = instaceName(name);
        if (!StringUtil.isNullOrEmpty(instance)) {
            server.setName(instance);
            cfg.setValue("instance-name", instance);
        }

        if (detectedVersion != null) {
            properties.setValue(PROP_DETECTED_VERSION, detectedVersion);
            cfg.setValue(PROP_DETECTED_VERSION, detectedVersion);
        }

        cfg.setValue(PROP_SERVICENAME, name);

        String discoverMsCluster = getPropertyByVersion(MS_CLUSTER_DISCOVERY, detectedVersion);

        String clusterId = null;
        if (discoverMsCluster != null) {
            clusterId = ClusterDetect.getClusterId();
            ConfigResponse clusterProps = ClusterDetect.getMssqlClusterProps(instance);
            if (clusterProps != null && !clusterProps.toProperties().isEmpty()) {
                cfg.merge(clusterProps, true);
                cfg.setValue(PROP_ORIGINAL_PLATFORM, getPlatformName());
                cfg.setValue(ACTIVE, ClusterDetect.pollInstanceOwner(instance));
            }
        }
        if (clusterId == null && !serverIsRunning) {
            // do not discover non-clustered servers that are down
            log.debug("Found a standalone server that is not running. Dropping it from discovery");
            return null;
        }
        if (cfg.getValue("sqlserver_name") == null) {
            cfg.setValue("sqlserver_name", getPlatformName());
        }
        server.setCustomProperties(properties);
        server.setProductConfig(cfg);
        server.setMeasurementConfig();
        server.setIdentifier(buildServerIdentifier(cfg.getValue("service_name"), clusterId));
        return server;
    }

    /*
     * Since mssql is a roaming resource, we need its identifier to be unique across the whole environment.
     * If the mssql server runs in a cluster, the unique id is <cluster-name>:<windows-service-name>
     * Otherwise, the unique id is <agent-token>:<windows-service-name>
     * Since the agent-token is not reachable within the plugin, using a place-holder that is filled in the server
     */
    private String buildServerIdentifier(String serviceName, String clusterId) {
        String identifier;
        if (clusterId != null && !"".equals(clusterId)) {
            identifier = clusterId + ":" + serviceName;
        }
        else {
            identifier = "%agentToken%:" + serviceName;
        }
        return identifier;
    }

    public static String instaceName(String name) {
        if (name == null) {
            return null;
        }
        String instance = name;
        if (name.startsWith("MSSQL" + SERVER_NAME_SEPARATOR)) {
            instance = name.substring(6);
        }
        return instance;
    }

    private static int getServiceStatus(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            debug(log,"[getServiceStatus] name='" + name + "' status='" + svc.getStatusString() + "'");
            return svc.getStatus();
        } catch (Win32Exception e) {
            debug(log,"[getServiceStatus] name='" + name + "' " + e);
            return Service.SERVICE_STOPPED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {

        ArrayList services = new ArrayList();

        Map<String, String> dbsDisk = new HashMap<String, String>();
        Map<String, String> dbsFile = new HashMap<String, String>();
        List<String> dbsFileNamesCMD = MsSQLDataBaseCollector.prepareSqlCommand(serverConfig.toProperties());
        dbsFileNamesCMD.add("-Q");
        dbsFileNamesCMD.add("SELECT name,filename FROM master..sysdatabases");
        List<List<String>> res;
        try {
            res = MsSQLDataBaseCollector.executeSqlCommand(dbsFileNamesCMD);

            for (List<String> line : res) {
                if (line.size() == 2) {
                    String path = line.get(1);
                    final String db = line.get(0);
                    debug(log,"===> " + db + " = " + path);
                    int i = path.indexOf("\\");
                    if (i != -1) {
                        dbsDisk.put(db, path.substring(0, i));
                        dbsFile.put(db, path);
                    }
                }
            }
        } catch (PluginException ex) {
            log.error("Unable to connect to the DB, review the user/password/sqlserver_name/instance options.", ex);
            return services;
        }
        debug(log,"===> dbsDisk = " + dbsDisk);
        debug(log,"===> dbsFile = " + dbsFile);
        if (dbsDisk.isEmpty()) {
            log.error("Unable to connect to the DB, review the user/password/sqlserver_name/instance options.");
            return services;
        }

        String sqlServerServiceName =
                    serverConfig.getValue(PROP_SERVICENAME, DEFAULT_SQLSERVER_SERVICE_NAME);

        List<ServiceInfo> servicesNames = new ArrayList<ServiceInfo>();
        String sqlServerMetricPrefix = "SQLServer"; // metric prefix in case of default instance

        String instanceName = DEFAULT_SQLSERVER_SERVICE_NAME;
        String version = serverConfig.getValue(PROP_DETECTED_VERSION);

        String msrsPrefix = getMsrsPrefix(version);

        if (sqlServerServiceName.equals(DEFAULT_SQLSERVER_SERVICE_NAME)) { // single instance
            String rpPrefix = "ReportServer";
            String olapPrefix = "MSAS12";

            // Concat the version number to the service info, which used to build the perfmon metrics name for MS
            // analysis server
            if (version.startsWith("2014")) {
                olapPrefix = "MSAS12";
            } else if (version.startsWith("2012")) {
                olapPrefix = "MSAS11";
            } else if (version.startsWith("2008")) {
                olapPrefix = "MSAS 2008";
            } else if (version.equals("2005")) {
                olapPrefix = "MSAS 2005";
            }
            servicesNames.add(new ServiceInfo("SQLSERVERAGENT", "Agent", "SQLAgent", MSSQL_AGENT_NAME));
            servicesNames.add(new ServiceInfo("ReportServer", REPORTING_SERVICES, rpPrefix, REPORTING_SERVICES));
            servicesNames.add(new ServiceInfo("MSSQLServerOLAPService", "Analysis Services", olapPrefix, "Analysis Services"));
        } else {    // multiple instances
            instanceName = sqlServerServiceName.substring(sqlServerServiceName.indexOf("$") + 1);
            sqlServerMetricPrefix = sqlServerServiceName;
            String agentWinServiceName = "SQLAgent" + SERVER_NAME_SEPARATOR + instanceName;
            servicesNames.add(new ServiceInfo(agentWinServiceName, "Agent", agentWinServiceName, MSSQL_AGENT_NAME));
            String reportServerWinServiceName = "ReportServer" + SERVER_NAME_SEPARATOR + instanceName;
            servicesNames.add(new ServiceInfo(reportServerWinServiceName, REPORTING_SERVICES,
                        reportServerWinServiceName, REPORTING_SERVICES));
            String analysisWinServiceName = "MSOLAP" + SERVER_NAME_SEPARATOR + instanceName;
            servicesNames.add(new ServiceInfo(analysisWinServiceName, "Analysis Services", analysisWinServiceName,
                        "Analysis Services"));
        }

        String activeValue = "";
        if(ClusterDetect.isCluster()) { //active value is irrelevant for single instance MSSQL, and invoking this method on a single instance will cause a NullPointerException
            activeValue = ClusterDetect.pollInstanceOwner(instanceName);
        }

        for (int i = 0; i < servicesNames.size(); i++) {
            ServiceInfo s = servicesNames.get(i);
            if (getServiceStatus(s.winServiceName) == Service.SERVICE_RUNNING) {
                debug(log,"[discoverServices] service='" + s.winServiceName + "' runnig");
                ServiceResource agentService = new ServiceResource();
                agentService.setType(this, s.type);
                agentService.setServiceName(s.serviceName);

                ConfigResponse cfg = new ConfigResponse();
                cfg.setValue(PROP_SERVICENAME, s.winServiceName);
                cfg.setValue("pref_prefix", s.metricsPrefix);
                cfg.setValue(ACTIVE, activeValue);
                cfg.setValue("instance", instanceName);
                if (s.type.equals(REPORTING_SERVICES)) {
                    cfg.setValue("MSRS", msrsPrefix);
                }

                agentService.setProductConfig(cfg);
                agentService.setMeasurementConfig();
                services.add(agentService);
            } else {
                debug(log,"[discoverServices] service='" + s.winServiceName + "' NOT runnig");
            }
        }

        // creating Database services
        try {
            String obj = sqlServerMetricPrefix + ":Databases";
            debug(log,"[discoverServices] obj='" + obj + "'");
            String[] instances = Pdh.getInstances(obj);
            debug(log,"[discoverServices] instances=" + Arrays.asList(instances));
            for (String dbName : instances) {
                if (!dbName.equals("_Total")) {
                    String path = dbsDisk.get(dbName);
                    String file = dbsFile.get(dbName);

                    ServiceResource service = new ServiceResource();
                    service.setType(this, DB_NAME);
                    service.setServiceName(dbName);

                    ConfigResponse cfg = new ConfigResponse();
                    cfg.setValue(MsSQLDetector.PROP_DB, dbName);
                    cfg.setValue("instance", instanceName);
                    cfg.setValue(ACTIVE, activeValue);
                    if (path != null) {
                        cfg.setValue("disk", path);
                        cfg.setValue("master.file", file);
                        service.setProductConfig(cfg);
                        service.setMeasurementConfig();
                        services.add(service);
                    }
                }
            }
        } catch (Win32Exception e) {
            debug(log,"[discoverServices] Error getting Databases pdh data for '" + sqlServerServiceName + "': " + e.getMessage(), e);
        }

        return services;
    }

    /**
     * Concat the version number to the service info, which used to build the perfmon metrics name for MS resport server
     */
    private String getMsrsPrefix(String version) {
        String msrsPrefix = "MSRS 2014 Windows Service";
        if (version != null) {
            if (version.startsWith("2012")) {
                msrsPrefix = "MSRS 2011 Windows Service";
            } else if (version.contains("R2")) {
                // make sure there is a space between the version number and the R2
                String splitVersion = (version.substring(0, version.indexOf("R2")) + " R2").replaceAll("\\s+", " ");
                msrsPrefix = "MSRS " + splitVersion + " Windows Service";
            } else {
                msrsPrefix = "MSRS " + version + " Windows Service";
            }
        }
        return msrsPrefix;
    }

    private class ServiceInfo {

        String winServiceName;
        String type;
        String metricsPrefix;
        String serviceName;

        public ServiceInfo(String winServiceName, String type, String metricsPrefix, String serviceName) {
            this.winServiceName = winServiceName;
            this.type = type;
            this.metricsPrefix = metricsPrefix;
            this.serviceName = serviceName;
        }
    }

    protected static void debug(Log _log, String msg) {
        if (_log.isDebugEnabled()) {
            _log.debug(msg.replaceAll("(-P,? ?)([^ ,]+)", "$1******").replaceAll("(pass[^=]*=)(\\w*)", "$1******"));
        }
    }

    protected static void debug(Log _log, String msg, Exception ex) {
        if (_log.isDebugEnabled()) {
            _log.debug(msg.replaceAll("(-P,? ?)([^ ,]+)", "$1******").replaceAll("(pass[^=]*=)(\\w*)", "$1******"), ex);
        }
    }

    /**
     * Get property, try search it with server version prefix.
     *
     * @param name
     * @param detectedVersion
     * @return
     */
    private String getPropertyByVersion(String name,
                                        String detectedVersion) {
        String propertyValue = getTypeProperty(getTypeInfo().getName(), name);

        // It might be that the property is prefix by server version number then search property in server specific
        // version
        if (propertyValue == null && detectedVersion != null) {
            propertyValue = getTypeProperty(getTypeInfo().getName(), detectedVersion.concat(".").concat(name));
        }
        return propertyValue;
    }

    /**
     * Get all occurrence of the property: in case property value is different between server versions the then the
     * property key will be prefix by version number
     *
     * @param name - property name
     * @return Map of property-value to version number.
     */
    private Map<String, String> getTypeMultiProperty(String name) {
        Map<String, String> propertiesMap = new HashMap<String, String>();

        String versions = getTypeProperty(getTypeInfo().getName(), "versions");
        if (versions != null) {
            String[] serverVersions;
            serverVersions = versions.split(",");
            for (String version : serverVersions) {
                String propertyValue = getTypeProperty(getTypeInfo().getName(), version.concat(".").concat(name));
                if (propertyValue != null) {
                    propertiesMap.put(propertyValue, version);
                }
            }
        }
        return propertiesMap;
    }

    public static String extractInstanceName(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        return serviceName.substring(serviceName.indexOf(SERVER_NAME_SEPARATOR) + 1);
    }

}
