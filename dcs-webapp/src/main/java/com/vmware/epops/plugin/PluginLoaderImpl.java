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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.ObjectModel;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

import com.vmware.epops.plugin.converter.NameMappingUtil;
import com.vmware.epops.plugin.converter.PluginConverterUtil;
import com.vmware.epops.plugin.converter.PropertyConfigOption;
import com.vmware.epops.plugin.model.PluginResourceType;
import com.vmware.epops.plugin.model.ResourceTypeConfig;
import com.vmware.epops.plugin.model.ResourceTypeMeasurement;

@Service
public class PluginLoaderImpl implements PluginLoader {

    private final static Logger LOGGER = LoggerFactory.getLogger(PluginLoaderImpl.class.getName());

    @Value("${epops.plugin.directory}")
    private String pluginDirName;

    @Autowired
    private PluginConverterUtil pluginConverterUtil;

    private ProductPluginManager productPluginManager;

    @PostConstruct
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "IS2_INCONSISTENT_SYNC",
                justification = "This method is called once after object creation and the only public api"
                            + "of this class is getModel() which is synchronized")
    private void initPluginManager()
        throws PluginException {
        File propFile = ProductPluginManager.PLUGIN_PROPERTIES_FILE;
        productPluginManager = new ProductPluginManager(propFile);
        productPluginManager.setRegisterTypes(true);

        if (propFile.canRead()) {
            LOGGER.info("Loaded custom properties from: {}", propFile);
        }
        File pluginDir = new File(pluginDirName);
        ProductPluginManager.setPdkPluginsDir(pluginDir.getAbsolutePath());

        /* workaround for the ConfigtrackPluginManager.initMonitor
        to understand that we are on a server, where there is Spring, so actual monitoring shouldn't start */
        ApplicationContext newAppContext = new GenericApplicationContext();
        Bootstrap.setAppContext(newAppContext);
    }

    @Override
    public synchronized Collection<PluginResourceType> getModel(List<String> activePluginFileNames)
        throws PluginException {

        try {
            productPluginManager.init();
            Collection<GenericPlugin> plugins = loadPlugins(activePluginFileNames);

            Map<String, PluginResourceType> pluginResourceTypes = new HashMap<>();
            for (GenericPlugin plugin : plugins) {
                TypeInfo[] types = plugin.getProductPlugin().getTypes();
                if (types == null) {
                    continue;
                }
                if (containsDuplicateMetrics(types, plugin.getName())) {
                    LOGGER.error("Plugin {} defines a type with duplicate metric names. Skipping.", plugin.getName());
                    continue;
                }
                for (TypeInfo type : types) {
                    String resourceName = type.getName();
                    if (!pluginResourceTypes.containsKey(resourceName)) {
                        PluginResourceType pluginResourceType = createPluginResourceType(type, plugin);
                        pluginResourceTypes.put(resourceName, pluginResourceType);
                        // in case this is a typeInfo of a different OS (Tomcat 6.0 on Linux /Tomcat 6.0 on Windows)
                        // need to
                        // update parents
                    } else {
                        pluginResourceTypes.get(resourceName).getParents().addAll(getSupportedParents(type));
                    }
                }
            }
            return pluginResourceTypes.values();
        } finally {
            productPluginManager.shutdown();
        }
    }

    private boolean containsDuplicateMetrics(TypeInfo[] types,
                                             String pluginName) {
        MeasurementPluginManager measurementManager = productPluginManager.getMeasurementPluginManager();
        for (TypeInfo type : types) {
            Set<String> metricNames = new HashSet<>();
            try {
                MeasurementInfo[] measurements = measurementManager.getMeasurements(type);
                for (int i = 0; i < measurements.length; i++) {
                    String name = alignMetricName(measurements[i].getName());
                    if (!metricNames.add(name)) {
                        LOGGER.error("Type {} of plugin {} defines two metrics with the name {}.", type.getName(),
                                    pluginName, measurements[i].getName());
                        return true;
                    }
                }
            } catch (PluginException e) {
                // No metrics
                continue;
            }
        }
        return false;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_CONVERT_CASE")
    private String alignMetricName(String metricName) {
        return metricName.replaceAll(" ", "").toLowerCase();
    }

    private Collection<GenericPlugin> loadPlugins(List<String> activePluginFileNames) {
        LOGGER.info("Loading Plugins");
        for (String pluginFilePath : activePluginFileNames) {
            LOGGER.debug("Loading Plugin: ", pluginFilePath);
            registerPluginJar(pluginFilePath);
        }
        return productPluginManager.getPlugins().values();
    }

    private void registerPluginJar(String pluginJar) {
        if (productPluginManager.isLoadablePluginName(pluginJar)) {
            try {
                productPluginManager.registerPluginJar(pluginJar, null);
                LOGGER.info("Plugin loaded: '{}'", pluginJar);
            } catch (PluginException e) {
                LOGGER.error("Unable to load plugin '{}'", pluginJar, e);
            }
        }
    }

    // Convert TypeInfo to PluginResourceType
    private PluginResourceType createPluginResourceType(TypeInfo type,
                                                        GenericPlugin plugin) {

        PluginResourceType resourceType = new PluginResourceType();
        resourceType.setName(NameMappingUtil.convertResourceTypeName(type.getName()));

        resourceType.setVirtual(isVirtualServer(type));
        resourceType.setLight(isLightService(type));
        resourceType.setRemote(isRemoteService(type));
        resourceType.setRoaming(isRoaming(type));
        resourceType.setParents(getSupportedParents(type));

        // Import and convert Resource ConfigOptions to ResourceTypeConfigs
        Set<ConfigOption> unifiedConfigurations = getConfiguration(plugin.getName(), type);
        List<ResourceTypeConfig> resourceTypeConfigList =
                    pluginConverterUtil.convertConfigurations(unifiedConfigurations);
        resourceType.setConfigProperties(resourceTypeConfigList);

        // Import and convert Resource MeasurementInfos to ResourceTypeMeasurements
        List<MeasurementInfo> measurements = getMeasurements(type);
        List<ResourceTypeMeasurement> measurementProperties =
                    pluginConverterUtil.convertMeasurements(measurements);
        resourceType.setMeasurementProperties(measurementProperties);

        // Import and convert Resource CustomProperies to ResourceTypeConfigs
        List<PropertyConfigOption> discoverableProperties = getDiscoverableProperties(type, plugin);
        List<ResourceTypeConfig> discoverableResourceTypeConfigs =
                    pluginConverterUtil.convertConfigurations(discoverableProperties);
        resourceType.setDiscoverableProperties(discoverableResourceTypeConfigs);

        return resourceType;
    }

    private boolean isRoaming(TypeInfo typeInfo) {
        if (typeInfo.getType() == TypeInfo.TYPE_SERVER) {
            return ((ServerTypeInfo) typeInfo).isRoaming();
        }
        if (typeInfo.getType() == TypeInfo.TYPE_SERVICE) {
            return ((ServiceTypeInfo) typeInfo).isRoaming();
        }
        return false;
    }

    private boolean isVirtualServer(TypeInfo typeInfo) {
        if (typeInfo.getType() == TypeInfo.TYPE_SERVER) {
            return ((ServerTypeInfo) typeInfo).isVirtual();
        }
        return false;
    }

    private boolean isRemoteService(TypeInfo typeInfo) {
        if (typeInfo.getType() == TypeInfo.TYPE_SERVICE) {
            return ObjectModel.REMOTE.equals(((ServiceTypeInfo) typeInfo).getModel());
        }
        return false;
    }

    private boolean isLightService(TypeInfo typeInfo) {
        if (typeInfo.getType() == TypeInfo.TYPE_SERVICE) {
            return ObjectModel.LIGHTWEIGHT.equals(((ServiceTypeInfo) typeInfo).getModel());
        }
        return false;
    }

    /* get a list of possible parents for this typeInfo.
     * the method takes under account service that is under a virtual server (a.k.a platform service)
     * and return platforms as its possible parents */
    private Set<String> getSupportedParents(TypeInfo type) {

        String[] supportedParents = null;
        switch (type.getType()) {
            case TypeInfo.TYPE_PLATFORM:
                break;
            case TypeInfo.TYPE_SERVER:
                supportedParents = type.getPlatformTypes();
                break;
            case TypeInfo.TYPE_SERVICE:
                ServerTypeInfo parent = ((ServiceTypeInfo) type).getServerTypeInfo();
                if (parent.isVirtual()) {
                    supportedParents = parent.getPlatformTypes(); // this is a platform service
                } else {
                    supportedParents = new String[1];
                    supportedParents[0] = parent.getName(); // this is a regular service (on server)
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported resource type: " + type.getType());
        }
        Set<String> parents = new HashSet<>();
        if (supportedParents != null) {
            for (int i = 0; i < supportedParents.length; i++) {
                String parent = NameMappingUtil.convertResourceTypeName(supportedParents[i]);
                parents.add(parent);
            }
        }
        return parents;
    }

    /**
     * @return All the needed configurations of a plugin. If the configuration is different between Linux and Windows,
     *         it returns all the configurations from both of them. It doesn't differentiate between Unix flavors
     *         configurations(AIX, solaris, etc...)
     */
    private Set<ConfigOption> getConfiguration(String pluginName,
                                               TypeInfo type) {

        List<ConfigOption> linuxResourceConfiguration = getResourceConfiguration(pluginName, type, Constants.OS_LINUX);
        List<ConfigOption> windowsResourceConfiguration =
                    getResourceConfiguration(pluginName, type, Constants.OS_WIN32);
        if (LOGGER.isDebugEnabled()) {
            compareConfigOptionsLists(pluginName, linuxResourceConfiguration, windowsResourceConfiguration);
        }

        Set<ConfigOption> unifiedPlatformConfigurations = unifyConfigurations(linuxResourceConfiguration,
                    windowsResourceConfiguration);

        if (type.getType() == TypeInfo.TYPE_SERVER && !type.getName().equals(Constants.AGENT_RESOURCE_TYPE)) {
            unifiedPlatformConfigurations.add(getInstallPathConfigOption());
        }
        return unifiedPlatformConfigurations;
    }

    private List<MeasurementInfo> getMeasurements(TypeInfo type) {
        MeasurementPluginManager measurementManager = productPluginManager.getMeasurementPluginManager();
        Map<String, MeasurementInfo> filteredMeasurements = new HashMap<String, MeasurementInfo>();
        try {
            MeasurementInfo[] measurements = measurementManager.getMeasurements(type);
            // Organize the templates to remove duplication
            for (int i = 0; i < measurements.length; i++) {
                filteredMeasurements.put(measurements[i].getName(), measurements[i]);
            }
        } catch (PluginException e) {
            LOGGER.debug("{} does not have any measurement infos", type.getName());
        }
        return new ArrayList<MeasurementInfo>(filteredMeasurements.values());
    }

    private List<PropertyConfigOption> getDiscoverableProperties(TypeInfo type,
                                                                 GenericPlugin plugin) {
        List<ConfigOption> discoverableProperties = new ArrayList<>();
        List<PropertyConfigOption> propertiesConfigOptions = new ArrayList<>();

        if (type.getType() == TypeInfo.TYPE_PLATFORM) {
            addNetworkProperties(discoverableProperties);
        }
        if (type.getType() == TypeInfo.TYPE_SERVER) {
            addHostProperties(discoverableProperties);
        }
        List<ConfigOption> customPropertiesOptions = plugin.getCustomPropertiesSchema(type).getOptions();
        discoverableProperties.addAll(customPropertiesOptions);

        for (ConfigOption configOption : discoverableProperties) {
            PropertyConfigOption propertyConfigOption = new PropertyConfigOption();
            propertyConfigOption.setConfigOption(configOption);
            propertiesConfigOptions.add(propertyConfigOption);
        }

        return propertiesConfigOptions;
    }

    // Install path should be config option for manually created server in vRops
    private ConfigOption getInstallPathConfigOption() {
        StringConfigOption configOption =
                    new StringConfigOption(Constants.INSTALL_PATH_NAME, Constants.INSTALL_PATH_DESC, "");
        configOption.setOptional(false);
        return configOption;
    }

    private void addNetworkProperties(List<ConfigOption> properties) {
        properties.add(new StringConfigOption(Constants.TYPE, Constants.TYPE_DESC, ""));
        properties.add(new StringConfigOption(Constants.FQDN, Constants.FQDN_DESC, ""));
        properties.add(new StringConfigOption(Constants.NETWORK_NAME, Constants.NETWORK_DESC, ""));
        properties.add(new StringConfigOption(Constants.NAME, Constants.NAME_DESC, ""));
        properties.add(new StringConfigOption(Constants.DESCRIPTION, Constants.DESCRIPTION_DESC, ""));
    }

    /**
     * Add properties from Type & Host Properties (for server)
     */
    private void addHostProperties(List<ConfigOption> properties) {
        properties.add(new StringConfigOption(Constants.NAME, Constants.NAME_DESC, ""));
        properties.add(new StringConfigOption(Constants.DESCRIPTION, Constants.DESCRIPTION_DESC, ""));
    }

    private void compareConfigOptionsLists(String pluginName,
                                           List<ConfigOption> linuxResourceConfiguration,
                                           List<ConfigOption> windowsResourceConfiguration) {
        Collections.sort(linuxResourceConfiguration, new ConfigOptionComparator());
        Collections.sort(windowsResourceConfiguration, new ConfigOptionComparator());

        if (linuxResourceConfiguration.size() != windowsResourceConfiguration.size()) {
            LOGGER.debug("Number of identifiers are not as expects for - {}", pluginName);
            return;
        }

        for (int i = 0; i < linuxResourceConfiguration.size(); i++) {
            ConfigOption configOptionLin = linuxResourceConfiguration.get(i);
            ConfigOption configOptionWin = windowsResourceConfiguration.get(i);
            if (!configOptionLin.getName().equals(configOptionWin.getName())) {
                LOGGER.debug("Config option - {} not equals to - {}",
                            Arrays.asList(configOptionLin.getName(), configOptionWin.getName()));
            }
        }
    }

    private static class ConfigOptionComparator implements Comparator<ConfigOption>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(ConfigOption conf1,
                           ConfigOption conf2) {
            return conf1.getName().toLowerCase(Locale.getDefault()).compareTo(
                        conf2.getName().toLowerCase(Locale.getDefault()));
        }
    }

    private Set<ConfigOption> unifyConfigurations(List<ConfigOption> linuxResourceConfiguration,
                                                  List<ConfigOption> windowsResourceConfiguration) {
        Set<ConfigOption> unifiedConfigOptions =
                    new LinkedHashSet<ConfigOption>(linuxResourceConfiguration); // order is important
        unifiedConfigOptions.addAll(windowsResourceConfiguration);

        return unifiedConfigOptions;
    }

    /**
     * Returns the configuration according to the platform
     */
    private List<ConfigOption> getResourceConfiguration(String pluginName,
                                                        TypeInfo pluginTypeInfo,
                                                        String
                                                        operatingSystem) {

        List<ConfigOption> configOptions = new LinkedList<ConfigOption>();

        List<ConfigOption> sharedConfig = getSharedConfig(pluginName, pluginTypeInfo);
        configOptions.addAll(sharedConfig);

        String mergedName = getMergedName(pluginTypeInfo, operatingSystem);

        List<ConfigOption> measureConfig = getMeasureConfig(mergedName, pluginTypeInfo);
        configOptions.addAll(measureConfig);

        return configOptions;
    }

    private String getMergedName(TypeInfo pluginTypeInfo,
                                 String operatingSystem) {
        String mergedName = null;
        if (pluginTypeInfo.getType() == TypeInfo.TYPE_PLATFORM) {
            mergedName = pluginTypeInfo.getName();
        } else {
            mergedName = pluginTypeInfo.getName() + " " + operatingSystem;
        }
        return mergedName;
    }

    private List<ConfigOption> getSharedConfig(String pluginName,
                                               TypeInfo pluginTypeInfo) {
        ConfigSchema sharedConfig = null;
        List<ConfigOption> result = new ArrayList<ConfigOption>();
        try {
            sharedConfig = productPluginManager.getConfigSchema(pluginName, pluginTypeInfo, new ConfigResponse());
            if (sharedConfig != null) {
                LOGGER.debug(String.format("Found shared configuration for pluginName: %s pluginTypeInfo:%s"
                            + " size: %s", pluginName, pluginTypeInfo, sharedConfig.getOptions().size()));
                result = sharedConfig.getOptions();
            }
        } catch (IllegalArgumentException | PluginNotFoundException e) {
            // happened in LateBindingConfigSchema
            LOGGER.debug("No shared configuration of type: " + pluginName);
        }
        return result;
    }

    private List<ConfigOption> getMeasureConfig(String mergedName,
                                                TypeInfo pluginTypeInfo) {

        MeasurementPluginManager measurementManager = productPluginManager.getMeasurementPluginManager();
        ConfigSchema measureConfig = null;
        List<ConfigOption> result = new ArrayList<ConfigOption>();
        try {
            measureConfig = measurementManager.getConfigSchema(mergedName, pluginTypeInfo, new ConfigResponse());
            if (measureConfig != null) {
                LOGGER.debug(String.format("Found measure configuration for mergedName: %s pluginTypeInfo: %s"
                            + " size: %s", mergedName, pluginTypeInfo, measureConfig.getOptions().size()));
                result = measureConfig.getOptions();
            }
        } catch (PluginNotFoundException e) {
            LOGGER.debug("No measure configuration of type:  " + mergedName);
            LOGGER.debug(e.getMessage(), e);
        }
        return result;
    }

}
