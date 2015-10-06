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
package com.springsource.hq.plugin.tcserver.plugin;

import com.springsource.hq.plugin.tcserver.plugin.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

import com.springsource.hq.plugin.tcserver.plugin.serverconfig.ServerXmlPropertiesRetriever;
import com.springsource.hq.plugin.tcserver.plugin.serverconfig.XmlPropertiesFileRetriever;
import com.springsource.hq.plugin.tcserver.plugin.utils.JmxUtils;
import com.springsource.hq.plugin.tcserver.plugin.utils.MxUtilJmxUtils;

public class TcRuntimeDetector extends MxServerDetector {

    public static final String SERVER_RESOURCE_CONFIG_CATALINA_HOME = "catalina.home";
    public static final String SERVER_RESOURCE_CONFIG_CATALINA_BASE = "catalina.base";
    public static final String SERVER_RESOURCE_CONFIG_TCVERSION = "tcversion";
    private static final String RELATIVE_PATH_CONF_CATALINA_PROPERTIES = "/conf/catalina.properties";
    private static final String CATALINA_BASE_PROP = "-Dcatalina.base=";
    private static final String CATALINA_HOME_PROP = "-Dcatalina.home=";
    private static final String TOMCAT_7_SPECIFIC_JAR = "lib/tomcat-util.jar";
    private static final String TOMCAT_8_SPECIFIC_JAR = "lib/tomcat-websocket.jar";
    private static final String DEFAULT_JMX_URL = "service:jmx:rmi:///jndi/rmi://127.0.0.1:6969/jmxrmi";
    private final Log logger = LogFactory.getLog(TcRuntimeDetector.class);
    private final JmxUtils mxUtil = new MxUtilJmxUtils();

    /**
     * Retrieves the listener properties from the server.xml file, then creates the jmx.url based on the properties.
     *
     * @param config The ConfigResponse object.
     * @param basePath The base file path of the configuration, "/conf/server.xml" is added to this.
     * @return Whether the configuration was found and set on the config response object.
     * @throws PluginException
     */
    private boolean configureListenerMxURL(ConfigResponse config,
                                           String basePath)
        throws PluginException {
        boolean found = false;
        XmlPropertiesFileRetriever propertiesRetriever = new ServerXmlPropertiesRetriever();
        try {
            Map<String, String> listenerProperties =
                        propertiesRetriever.getPropertiesFromFile(basePath + "/conf/server.xml", "Listener",
                                    "className", "com.springsource.tcserver.serviceability.rmi.JmxSocketListener");
            if (!listenerProperties.isEmpty()) {
                String bindAddressValue = getValueFromPropertiesFile(basePath, listenerProperties.get("bind"));
                String portValue = getValueFromPropertiesFile(basePath, listenerProperties.get("port"));
                config.setValue(mxUtil.getJmxUrlProperty(), "service:jmx:rmi:///jndi/rmi://" + bindAddressValue + ":"
                            + portValue + "/jmxrmi");
                found = true;
            }
        } catch (PluginException e) {
            // the properties were not accessible
            config.setValue(mxUtil.getJmxUrlProperty(), DEFAULT_JMX_URL);
            logger.warn("Unable to retrieve properties for discovery, using default " + mxUtil.getJmxUrlProperty()
                        + "=" + DEFAULT_JMX_URL);
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
        }
        return found;
    }

    @Override
    protected boolean includeNoneKeyPropertiesInServiceName() {
        return false;
    }

    private String getCatalinaBase(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(CATALINA_BASE_PROP)) {
                return arg.substring(CATALINA_BASE_PROP.length());
            }
        }
        return null;
    }

    private String getCatalinaHome(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(CATALINA_HOME_PROP)) {
                return arg.substring(CATALINA_HOME_PROP.length());
            }
        }
        return null;
    }

    /**
     * Overridden to pass the value of catalina.home to the isInstallTypeVersion method, as opposed to the installpath,
     * which is specified by catalina.base. This is necessary for tc Server, where there may be multiple tc Runtime
     * instances with different catalina.base values sharing the libraries that are used to determine version. The
     * location of those VERSION_FILES needs to be resolved relative to catalina.home. Also overridden to set the
     * control config automatically, so control ops can be executed with default config values
     *
     * @param platformConfig
     * @return
     * @throws org.hyperic.hq.product.PluginException
     */
    @Override
    public final List<ServerResource> getServerResources(ConfigResponse platformConfig)
        throws PluginException {
        setPlatformConfig(platformConfig);

        List<ServerResource> servers = new ArrayList<ServerResource>();
        @SuppressWarnings("rawtypes")
        List procs = getServerProcessList();

        for (Object proc : procs) {
            MxProcess process = (MxProcess) proc;
            String[] processArgs = process.getArgs();

            String catalinaBase = getCatalinaBase(processArgs);
            String catalinaHome = getCatalinaHome(processArgs);

            if (!isHqTcRuntime(catalinaBase) && isTcRuntimeInstance(catalinaHome, catalinaBase)) {
                ServerResource serverResource =
                            createTcRuntimeServerResource(catalinaHome, catalinaBase, processArgs, process);
                if (serverResource != null) {
                    servers.add(serverResource);
                }
            }
        }

        return servers;
    }

    /**
     * Get the server name for server resource found.
     *
     * @param defaultServerName The default server name.
     * @return
     */
    protected String getServerName(String defaultServerName) {
        String serverName = getTypeProperties().getProperty("TC_SERVER_NAME");
        if (serverName == null) {
            serverName = defaultServerName;
        }
        return serverName;
    }

    /**
     * Get the description for server.
     *
     * @param defaultDescription The default description.
     * @return
     */
    protected String getServerDescription(String defaultDescription) {
        String serverDescription = getTypeProperties().getProperty("TC_SERVER_DESCRIPTION");
        if (serverDescription == null) {
            serverDescription = defaultDescription;
        }
        return serverDescription;
    }

    private ServerResource createTcRuntimeServerResource(String catalinaHome,
                                                         String catalinaBase,
                                                         String[] processArgs,
                                                         MxProcess process)
        throws PluginException {

        String query = PROC_JAVA + ",Args.*.eq=-D" + getProcHomeProperty() + "=" + catalinaBase;

        // Create the server resource
        ServerResource server = newServerResource(catalinaBase);
        server.setDescription(getServerDescription(server.getInstallPath()));
        adjustClassPath(catalinaBase);

        ConfigResponse productConfig = new ConfigResponse();
        ConfigSchema schema = getConfigSchema(getTypeInfo().getName(), ProductPlugin.CFGTYPE_IDX_PRODUCT);

        if (schema != null) {
            ConfigOption option = schema.getOption(PROP_PROCESS_QUERY);
            if (option != null) {
                productConfig.setValue(option.getName(), query);
            }
        }

        if (process.getURL() != null) {
            productConfig.setValue(mxUtil.getJmxUrlProperty(), process.getURL());
        } else {
            String[] args = process.getArgs();
            for (String arg : args) {
                if (configureListenerMxURL(productConfig, catalinaBase)) {
                    break;
                } else if (configureMxURL(productConfig, arg)) {
                    break;
                } else {
                    configureLocalMxURL(productConfig, arg, query);
                }
            }
        }

        storeProcessUserAndGroup(process, productConfig);

        productConfig.setValue(SERVER_RESOURCE_CONFIG_CATALINA_BASE, catalinaBase);
        productConfig.setValue(SERVER_RESOURCE_CONFIG_CATALINA_HOME, catalinaHome);

        if (new File(catalinaHome, TOMCAT_8_SPECIFIC_JAR).exists()) {
            productConfig.setValue(SERVER_RESOURCE_CONFIG_TCVERSION, "8");
        } else if (new File(catalinaHome, TOMCAT_7_SPECIFIC_JAR).exists()) {
            productConfig.setValue(SERVER_RESOURCE_CONFIG_TCVERSION, "7");
        } else {
            productConfig.setValue(SERVER_RESOURCE_CONFIG_TCVERSION, "6");
        }

        // default anything not auto-configured
        productConfig.setValue("jmx.password", getJMXPassword(catalinaBase));
        setProductConfig(server, productConfig);
        discoverServerConfig(server, process.getPid());

        server.setMeasurementConfig();
        server.setName(getTypeInfo().getName() + " - "
                    + getPlatformConfig().getValue(ProductPlugin.PROP_PLATFORM_NAME, getPlatformName()) + ":"
                    + catalinaBase);

        return server;
    }

    private void storeProcessUserAndGroup(MxProcess process,
                                          ConfigResponse config)
        throws InvalidOptionException, InvalidOptionValueException,
        PluginException {
        String user = null;
        String group = null;
        try {
            ProcCredName procCredName = getSigar().getProcCredName(process.getPid());
            user = procCredName.getUser();
            group = procCredName.getGroup();
        } catch (SigarException se) {
            logger.warn("Failed to get proc cred names for tc Server process " + process.getPid());
            if (logger.isDebugEnabled()) {
                logger.debug(se);
            }

            ProcCred procCred;
            try {
                procCred = getSigar().getProcCred(process.getPid());
                user = String.valueOf(procCred.getUid());
                group = String.valueOf(procCred.getGid());
            } catch (SigarException e) {
                logger.warn("Failed to get proc cred for tc Server process " + process.getPid());
            }
        } finally {
            logger.info("Using " + user + "/" + group + " as the user/group for tc Runtime instance process with pid: "
                        + process.getPid());
        }

        config.setValue(Utils.SERVER_RESOURCE_CONFIG_PROCESS_USERNAME, user);
        config.setValue(Utils.SERVER_RESOURCE_CONFIG_PROCESS_GROUP, group);
    }

    private String getValueFromPropertiesFile(String basePath,
                                              String property)
        throws PluginException {
        String propertyValue = property;
        if (property.startsWith("${") && property.endsWith("}")) {
            String propertyKey = property.substring(2, property.length() - 1);
            File catalinaProperties = new File(basePath, RELATIVE_PATH_CONF_CATALINA_PROPERTIES);
            InputStream catalinaPropertiesInputStream = null;
            try {
                catalinaPropertiesInputStream = new FileInputStream(catalinaProperties);
                Properties properties = new Properties();
                properties.load(catalinaPropertiesInputStream);
                propertyValue = properties.getProperty(propertyKey);
            } catch (FileNotFoundException e) {
                throw new PluginException("Unable to find " + catalinaProperties.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new PluginException("Failed to read " + catalinaProperties.getAbsolutePath(), e);
            } finally {
                if (catalinaPropertiesInputStream != null) {
                    try {
                        catalinaPropertiesInputStream.close();
                    } catch (IOException ioe) {
                        // Do nothing
                    }
                }
            }
        }
        return propertyValue;
    }

    private String getJMXPassword(String catalinaBase) {
        String password = "";
        BufferedReader reader = null;
        String passwordFilePath = catalinaBase + "/conf/" + "jmxremote.password";
        try {
            File passwordFile = new File(passwordFilePath);
            reader = new BufferedReader(new FileReader(passwordFile));
            String line = reader.readLine();
            while (line != null) {
                if (line.trim().startsWith("admin")) {
                    password = line.substring(5, line.length()).trim();
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            logger.debug("Unable to locate file: " + passwordFilePath + ". Error message: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.debug("Problem loading file: " + passwordFilePath + ". Error message: " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return password;
    }

    private boolean isHqTcRuntime(String catalinaBase) {
        File hq = findVersionFile(new File(catalinaBase), Pattern.compile("hq-common.*\\.jar"));
        return hq != null;
    }

    protected boolean isTcRuntimeInstance(String catalinaHome,
                                          String catalinaBase) {
        if (catalinaHome == null) {
            return false;
        }

        if (catalinaBase == null) {
            return false;
        }

        return isInstallTypeVersion(catalinaHome) || isInstallTypeVersion(catalinaBase);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {
        try {
            return super.discoverServices(serverConfig);
        } catch (PluginException ex) {
            if (logger.isDebugEnabled()) {
                logger.error("[discoverServices] Failed to discover services." + ex, ex);
            } else {
                logger.error("[discoverServices] Failed to discover services." + ex);
            }
            return new ArrayList();
        }
    }
}
