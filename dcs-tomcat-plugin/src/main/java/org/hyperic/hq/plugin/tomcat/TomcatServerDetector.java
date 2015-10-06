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

package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigResponse;

public class TomcatServerDetector extends MxServerDetector {

    private static final String TOMCAT_VERSION = "Tomcat Version";
    private static final String TOMCAT_PARAMS_KEY = "\\Parameters\\Java";
    private static final String TOMCAT_SERVICE_KEY = "SOFTWARE\\Apache Software Foundation\\Procrun 2.0";

    private static final String PTQL_QUERY_WIN32 = "Pid.Service.eq=%service_name%";

    private static final String CATALINA_BASE_PROP = "-Dcatalina.base=";

    private static final String TOMCAT_DEFAULT_URL = "service:jmx:rmi:///jndi/rmi://localhost:6969/jmxrmi";

    private static final String PTQL_CONFIG_OPTION = SigarMeasurementPlugin.PTQL_CONFIG;

    private static final String CATALINA_HOME_PROP = "-Dcatalina.home=";

    private static final String DATA_SOURCEPOOL_PLACEHOLDER = "data_sourcepool_context_placeholder";

    private static final String TOMCAT_55_VERSION_FILE = "server" + File.separator + "lib" + File.separator
                + "catalina-storeconfig.jar";
    private static final String TOMCAT_6_VERSION_FILE = "lib" + File.separator + "catalina-ha.jar";
    private static final String TOMCAT_7_VERSION_FILE = "lib" + File.separator + "tomcat-api.jar";
    private static final String[] versionFiles =
    { TOMCAT_55_VERSION_FILE, TOMCAT_6_VERSION_FILE, TOMCAT_7_VERSION_FILE };

    private static final Map<String, String> versionFileToVersionMap;
    private static final String TC_SERVER_VERSION_FILE = "lib/tcServer.jar";

    static {
        versionFileToVersionMap = new HashMap<String, String>(4);
        versionFileToVersionMap.put(TOMCAT_55_VERSION_FILE, "1.0");
        versionFileToVersionMap.put(TOMCAT_55_VERSION_FILE, "5.5");
        versionFileToVersionMap.put(TOMCAT_6_VERSION_FILE, "6.0");
        versionFileToVersionMap.put(TOMCAT_7_VERSION_FILE, "7.0");
    }

    private Log log = LogFactory.getLog(TomcatServerDetector.class);

    private ServerResource getServerResource(String win32Service,
                                             List options)
        throws PluginException {
        if (!isWin32ServiceRunning(win32Service)) {
            log.debug(win32Service + " is not running, skipping.");
            return null;
        }
        String path;
        String[] args = (String[]) options.toArray(new String[0]);
        String catalinaBase = getCatalinaBase(args);
        if (catalinaBase == null) {
            // no catalina base found
            log.error("No Catalina Base found for service " + win32Service + ". Skipping..");
            return null;
        } else {
            File catalinaBaseDir = new File(catalinaBase);
            if (catalinaBaseDir.exists()) {
                log.debug("Successfully detected Catalina Base for service: " + catalinaBase + " options=" + options);
                path = catalinaBaseDir.getAbsolutePath();
            } else {
                log.error("Resolved catalina base " + catalinaBase +
                            " is not a valid directory. Skipping Tomcat service " + win32Service);
                return null;
            }
        }
        String tomcatVersion =
                    findVersionByVersionFiles(catalinaBase);
        if (tomcatVersion == null) {
            return null;
        }

        ServerResource server = createServerResource(path);
        // Set PTQL query
        ConfigResponse config = new ConfigResponse();
        config.setValue(MxUtil.PROP_JMX_URL, TOMCAT_DEFAULT_URL);
        for (int i = 0; i < args.length; i++) {
            if (configureMxURL(config, args[i])) {
                break;
            }
        }
        ConfigResponse customProperties = server.getCustomProperties();
        if (customProperties == null) {
            customProperties = new ConfigResponse();
        }
        customProperties.setValue(TOMCAT_VERSION, tomcatVersion);
        config.setValue(Win32ControlPlugin.PROP_SERVICENAME, win32Service);
        config.setValue(PTQL_CONFIG_OPTION, PTQL_QUERY_WIN32);
        server.setName(prepareWinServerName(server, tomcatVersion, win32Service, getPlatformConfig()));

        String processQuery = config.getValue(MxServerDetector.PROP_PROCESS_QUERY);
        if (processQuery != null ){
	        String query = Metric.translate(processQuery,config);
	        long[] pids = getPids(query);
	        if (pids!= null && pids.length != 0){
	            populateListeningPorts(pids[0], config, true);
	        }
        }

        server.setProductConfig(config);
        server.setCustomProperties(customProperties);
        server.setMeasurementConfig();

        return server;
    }

    private String[] getServicesFromRegistry() {
        RegistryKey key = null;
        String[] services = null;
        try {
            key = RegistryKey.LocalMachine.openSubKey(TOMCAT_SERVICE_KEY);
            services = key.getSubKeyNames();
        } catch (Win32Exception e) {
            // no tomcat services installed
        } finally {
            if (key != null) {
                key.close();
            }
        }
        return services;
    }

    /**
     * Helper method to discover Tomcat server paths using the Windows registry
     */
    private Map getServerRegistryMap() {
        Map serverMap = new HashMap();

        String[] services = getServicesFromRegistry();
        // return empty map if no windows services are found
        if (services == null) {
            return serverMap;
        }

        for (int i = 0; i < services.length; i++) {
            log.debug("Detected Tomcat service " + services[i]);
            List options = new ArrayList();
            RegistryKey key = null;
            try {
                key = RegistryKey.LocalMachine.openSubKey(TOMCAT_SERVICE_KEY + "\\" + services[i] + TOMCAT_PARAMS_KEY);
                key.getMultiStringValue("Options", options);
            } catch (Win32Exception e) {
                log.error("Failed to find Java parameters for Tomcat service " + services[i]);
                // skip current service
                continue;
            } finally {
                if (key != null) {
                    key.close();
                }
            }
            serverMap.put(services[i], options);
        }
        return serverMap;
    }

    @Override
    protected boolean isInstallTypeVersion(MxProcess process) {
        return getTomcatVersion(process) != null && !isTcServer(process);
    }

    /**
     * Checks if the given process is of type TC server.
     * If it is, Tomcat plugin shouldn't monitor it
     */
    private boolean isTcServer(MxProcess process) {
        final String[] processArgs = process.getArgs();
        String catalinaHome = getCatalinaHome(processArgs);
        String catalinaBase = getCatalinaBase(processArgs);
        String typeName = getTypeInfo().getName();

        File homeVersionFile = new File(catalinaHome, TC_SERVER_VERSION_FILE);
        File baseVersionFile = new File(catalinaBase, TC_SERVER_VERSION_FILE);
        if ((homeVersionFile.exists() || baseVersionFile.exists()) &&
                findVersionFile(new File(catalinaBase), Pattern.compile("hq common.*\\.jar")) == null) {
            getLog().debug("[isInstallTypeVersion] '" + getTypeInfo().getName()
                    + " [" + process.getInstallPath() + "]' is a SpringSource tc Runtime");
            return true;
        } else {
            getLog().debug("[isInstallTypeVersion] '" + getTypeInfo().getName()
                    + " [" + process.getInstallPath() + "]' is not a SpringSource tc Runtime");
        }
        return false;
    }

    protected String getCatalinaHome(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(CATALINA_HOME_PROP)) {
                return args[i].substring(CATALINA_HOME_PROP.length());
            }
        }
        return null;
    }

    @Override
    protected String getProcQuery(String path) {
        String query = super.getProcessQuery();
        if (path != null) {
            query += path;
        }
        return query;
    }

    @Override
    protected boolean isInstallTypeVersion(String installpath) {
        if (findVersionByVersionFiles(installpath) != null) {
            return true;
        }
        return false;
    }

    /**
     * Auto scan
     */
    @Override
    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {
        List servers = super.getServerResources(platformConfig);

        // if we are on windows, take a look at the registry for autodiscovery
        if (isWin32()) {
            Map registryMap = getServerRegistryMap();
            // convert registry options to server value types
            for (Iterator it = registryMap.keySet().iterator(); it.hasNext();) {
                String serviceName = (String) it.next();
                List options = (List) registryMap.get(serviceName);
                ServerResource server = getServerResource(serviceName, options);
                if (server != null) {
                    servers.add(server);
                }
            }
        }

        // set control config for all servers
        if (servers != null) {
            for (Object server : servers) {
                ServerResource serverResource = ((ServerResource) server);
                serverResource.setControlConfig();
            }
        }

        return servers;
    }

    private String getBootstrapJar(String[] args) {
        String res = null;
        for (int i = 0; (i < args.length) && (res == null); i++) {
            if (args[i].equalsIgnoreCase("-classpath")) {
                String[] cp = args[i + 1].split(File.pathSeparator);
                for (int c = 0; (c < cp.length) && (res == null); c++) {
                    if (cp[c].endsWith("bootstrap.jar")) {
                        res = cp[c];
                    }
                }
            }
        }
        log.debug("[getBootstrapJar] res='" + res + "'");
        return res;
    }

    private String getCatalinaBase(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(CATALINA_BASE_PROP)) {
                return args[i].substring(CATALINA_BASE_PROP.length());
            }
        }
        return null;
    }

    @Override
    protected void setProductConfig(ServerResource server,
                                    ConfigResponse config,
                                    long pid) {
        populateListeningPorts(pid, config, true);
        super.setProductConfig(server, config);
    }

    @Override
    protected ServerResource getServerResource(MxProcess process) {
        ServerResource server = super.getServerResource(process);

        ConfigResponse productConfig = server.getProductConfig();
        if (productConfig == null) {
            productConfig = new ConfigResponse();
        }
        String tomcatVersion = getTomcatVersion(process);
        if (tomcatVersion == null) {
            return null;
        }
        ConfigResponse customProperties = server.getCustomProperties();
        if (customProperties == null) {
            customProperties = new ConfigResponse();
        }
        server.setName(prepareMxServerName(server, getPlatformConfig()));
        customProperties.setValue(TOMCAT_VERSION, tomcatVersion);
        server.setCustomProperties(customProperties);
        server.setProductConfig(productConfig);
        server.setMeasurementConfig();
        return server;
    }

    /**
     * We want this ServerDetector going first to win the battle for monitoring HQ Server in an EE env TODO more elegant
     * way to do this?
     */
    @Override
    public int getScanOrder() {
        return 0;
    }

    private String getTomcatVersion(MxProcess process) {
        String bootstrapJar = getBootstrapJar(process.getArgs());
        String tomcatVersion = null;

        if (bootstrapJar != null) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(bootstrapJar);
                Attributes attributes = jarFile.getManifest().getMainAttributes();
                jarFile.close();
                tomcatVersion = validateVersion(attributes.getValue("Specification-Version"));
                if (tomcatVersion == null) {
                    String implVersion = attributes.getValue("Implementation-Version");
                    if (implVersion != null) {
                        String[] implVersionSplit = implVersion.split("\\.");
                        if (implVersionSplit != null && implVersionSplit.length > 1) {
                            tomcatVersion = validateVersion(implVersionSplit[0] + "." + implVersionSplit[1]);
                        }
                    }
                }
            } catch (IOException e) {
                log.debug("Error getting Tomcat version (" + e + ")", e);
            } finally {
                try {
                    if(jarFile != null){
                        jarFile.close();
                    }
                } catch (IOException e){
                    log.error("Failed to close bootstrap Jar file.");
                }
            }
        }
        String catalinaHome = getCatalinaHome(process.getArgs());
        String catalinaBase = getCatalinaBase(process.getArgs());
        if (tomcatVersion == null) {
            tomcatVersion = findVersionByVersionFiles(catalinaBase);
        }
        if (tomcatVersion == null) {
            tomcatVersion = findVersionByVersionFiles(catalinaHome);
        }
        return tomcatVersion;
    }

    private String findVersionByVersionFiles(String installpath) {
        String tomcatVersion = null;
        File instPath = new File(installpath);
        if (installpath == null) {
            return null;
        }
        if (instPath.isFile() && !instPath.isDirectory()) {
            instPath = instPath.getParentFile();
        }
        for (int i = 0; i < versionFiles.length; i++) {
            File file = (instPath != null) ? new File(instPath, versionFiles[i]) :
                        new File(versionFiles[i]);
            if (!file.exists()) {
                String[] expanded = PluginLoader.expand(file);
                if ((expanded == null) || (expanded.length == 0)) {
                    getLog().debug(file + " does not exist, skipping");
                    continue;
                }
                else {
                    getLog().debug(versionFiles[i] + " matches -> " + expanded[0]);
                    tomcatVersion = versionFileToVersionMap.get(versionFiles[i]);
                }
            } else {
                tomcatVersion = versionFileToVersionMap.get(versionFiles[i]);
            }
        }
        return tomcatVersion;
    }

    private String validateVersion(String tomcatVersion) {
        if (tomcatVersion == null
                    || (!"1.0".equals(tomcatVersion) && !"6.0".equals(tomcatVersion) && !"7.0".equals(tomcatVersion))) {
            return tomcatVersion;
        }
        return null;
    }

    private void populateListeningPorts(long pid,
                                        ConfigResponse productConfig,
                                        boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }

    private String prepareWinServerName(ServerResource server,
                                        String tomcatVersion,
                                        String win32Service,
                                        ConfigResponse platformConfig) {
        String platformIdentifier = getPlatformIdentifierName(platformConfig);
        String serverName =
                    String.format("%s %s %s" + HQConstants.RESOURCE_NAME_DELIM + "%s", server.getType(), tomcatVersion, win32Service, platformIdentifier);
        return serverName;
    }

    private String prepareMxServerName(ServerResource server,
                                       ConfigResponse platformConfig) {
        String platformIdentifier = getPlatformIdentifierName(platformConfig);
        String serverName = String.format("%s" + HQConstants.RESOURCE_NAME_DELIM + "%s:%s", server.getType(), platformIdentifier,
                    server.getInstallPath());
        return serverName;
    }

    private String getPlatformIdentifierName(ConfigResponse platformConfig) {
        return platformConfig.getValue(ProductPlugin.PROP_PLATFORM_DISPLAY_NAME, getPlatformName());
    }

    @Override
    protected boolean includeNoneKeyPropertiesInServiceName() {
        return false;
    }
}
