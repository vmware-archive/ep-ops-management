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

package org.hyperic.hq.configuration.agent.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.agent.server.CommandDispatcher;
import org.hyperic.hq.agent.server.CommandInvokerUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.autoinventory.agent.client.AICommandsUtils;
import org.hyperic.hq.configuration.agent.client.ConfigurationCommandsClient;
import org.hyperic.hq.configuration.agent.commands.Configuration_args;
import org.hyperic.hq.configuration.agent.repository.ConfigurationInfoTranslator;
import org.hyperic.hq.configuration.agent.repository.ResourceRepositoryException;
import org.hyperic.hq.configuration.agent.repository.ResourceRepositoryService;
import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo;
import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo.ChangeType;
import org.hyperic.hq.configuration.agent.repository.model.MergedSchedulingInfo;
import org.hyperic.hq.configuration.agent.repository.model.ResourceKey;
import org.hyperic.hq.configuration.agent.repository.model.SchedulingInfo;
import org.hyperic.hq.measurement.agent.MeasurementCommandsAPI;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurementsById_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.scheduling.SchedulingException;

/**
 * Configuration Commands Service is in charge of the actual business logic implementation of configuration commands.
 */
public class ConfigurationCommandsService implements ConfigurationCommandsClient {

    private static final Log log = LogFactory.getLog(ConfigurationCommandsService.class);

    private static final int DEFAULT_REVISION_NUMBER = 1;

    private static final String LWO_INSTANCE_DELIMITER = ":";

    private final ResourceRepositoryService resourceRepositoryService;
    private final AgentDaemon agentDaemon;
    private final CommandDispatcher commandDispatcher;

    public ConfigurationCommandsService(AgentDaemon agentDaemon) {
        this.agentDaemon = agentDaemon;
        this.commandDispatcher = agentDaemon.getCommandDispatcher();
        resourceRepositoryService = ResourceRepositoryService.getInstance();

    }

    public void configure(Configuration_args configurationArgs)
        throws AgentRemoteException {
        if (configurationArgs.isDeletedResource()) {
            delete(configurationArgs);
        } else {
            configurationAndScheduling(configurationArgs);
        }
    }

    public void configurationAndScheduling(Configuration_args configurationArgs)
        throws AgentRemoteException {

        try {
            ConfigurationInfo newConfigInfo = ConfigurationInfoTranslator.translate(configurationArgs, agentDaemon);
            handleSyncMode(newConfigInfo);
            ChangeType changeType = resourceRepositoryService.compareAndSwap(newConfigInfo);
            if (resourceRepositoryService.isResourceReadyForConfigurationAndScheduling(newConfigInfo.getResourceKey())) {
                handlePushRuntimeDiscoveryConfig(changeType, newConfigInfo);
                handleScheduling(changeType, newConfigInfo);
                handleDescendants(changeType, newConfigInfo.getResourceKey());
            }
        } catch (Exception e) {
            log.error("Exception in configurationAndScheduling() method", e);
            log.error(buildExceptionLogMessage(e, "Error:"));
        }
    }

    private void handleSyncMode(ConfigurationInfo config) {
        if (config == null || TypeInfo.TYPE_SERVER != config.getTypeInfo().getType()) {
            return;
        }
        if (!resourceRepositoryService.isResourceExists(config.getResourceKey())) {
            agentDaemon.getSyncModeManager().setSyncMode(true);
        }
    }

    public void delete(Configuration_args configurationArgs)
        throws AgentRemoteException {

        try {
            ConfigurationInfo initConfigInfo = new ConfigurationInfo();
            ConfigurationInfoTranslator.translateResourceInternalId(configurationArgs, initConfigInfo);
            int resourceInternalId = initConfigInfo.getResourceInternalId();

            ConfigurationInfo configInfo = resourceRepositoryService.getConfigInfoByInternalId(resourceInternalId);
            if (configInfo != null) {
                ResourceKey resourceKey = configInfo.getResourceKey();
                if (resourceRepositoryService.isResourceReadyForConfigurationAndScheduling(resourceKey)) {
                    UnScheduleAllDescendants(resourceKey);
                    triggerUnScheduleMeasurementsCommand(configInfo);
                }
                resourceRepositoryService.removeResource(resourceKey);
                // for a deleted server, or deleted platform with virtual servers, this will push
                // runtimeDiscoveryConfig that will remove the old internal id from the ConfigStorage.
                handlePushRuntimeDiscoveryConfig(ChangeType.CONFIGURATION, configInfo);
            } else {
                // The resource is not in the Configuration Tree but it has metrics that are still scheduled.
                // That Happens when the Configuration Tree is initialized due to agent restart.
                triggerUnscheduleMeasurementsByIdCommand(resourceInternalId);
            }

        } catch (Exception e) {
            log.error("Exception in delete method", e);
            log.error(buildExceptionLogMessage(e, "Error:"));
        }
    }

    private void handlePushRuntimeDiscoveryConfig(ChangeType changeType,
                                                  ConfigurationInfo config)
        throws AgentRemoteException, ResourceRepositoryException {

        if (isSyncMode() || ChangeType.CONFIGURATION.equals(changeType) || ChangeType.BOTH.equals(changeType)) {
            // Send push runtime config only for servers.
            // TODO: Add support for manullay created services.
            if (TypeInfo.TYPE_SERVER == config.getTypeInfo().getType()) {

                AgentRemoteValue arg =
                            buildPushRuntimeDiscoveryConfigArgs(config, config.getTypeInfo().getType(),
                                        config.getResourceKind());
                CommandInvokerUtil.triggerCommand(AICommandsAPI.command_pushRuntimeDiscoveryConfig, arg);
            } else if (TypeInfo.TYPE_PLATFORM == config.getTypeInfo().getType() &&
                        !CollectionUtils.isEmpty(config.getVirtualServerConfiguration())) {

                Map<String, Map<String, String>> virtualServers = config.getVirtualServerConfiguration();
                for (Entry<String, Map<String, String>> virtualServerEntry : virtualServers.entrySet()) {
                    AgentRemoteValue arg =
                                buildPushRuntimeDiscoveryConfigArgs(config, TypeInfo.TYPE_SERVER,
                                            virtualServerEntry.getKey());
                    CommandInvokerUtil.triggerCommand(AICommandsAPI.command_pushRuntimeDiscoveryConfig, arg);
                }
            }
        }
    }

    private void handleScheduling(ChangeType changeType,
                                  ConfigurationInfo configInfo)
        throws AgentRemoteException,
        AgentRunningException, PluginException, ResourceRepositoryException, SchedulingException {
        if (isSyncMode() || !ChangeType.EQUAL.equals(changeType)) {
            if (CollectionUtils.isEmpty(configInfo.getScheduling())
                        && CollectionUtils.isEmpty(configInfo.getLwoScheduling())) {
                triggerUnScheduleMeasurementsCommand(configInfo);
            } else {
                triggerScheduleMeasurementsCommand(configInfo);
            }
        }
    }

    private void handleDescendants(ChangeType changeType,
                                   ResourceKey resourceKey) {
        if (!isSyncMode() && ChangeType.CONFIGURATION.equals(changeType) || ChangeType.BOTH.equals(changeType)) {
            List<ConfigurationInfo> descendants = resourceRepositoryService.getResourceDescendents(resourceKey);
            for (ConfigurationInfo descendant : descendants) {
                try {
                    handlePushRuntimeDiscoveryConfig(changeType, descendant);
                    handleScheduling(changeType, descendant);
                } catch (Exception e) {
                    log.error(buildExceptionLogMessage(e, "Failed to handle " + resourceKey
                                + " descendat configuration. Skipping."), e);
                    continue;
                }
            }
        }
    }

    private void UnScheduleAllDescendants(ResourceKey resourceKey) {
        List<ConfigurationInfo> descendants = resourceRepositoryService.getResourceDescendents(resourceKey);
        for (ConfigurationInfo descendant : descendants) {
            try {
                triggerUnScheduleMeasurementsCommand(descendant);
            } catch (Exception e) {
                log.error(buildExceptionLogMessage(e, "Failed to handle " + resourceKey
                            + " descendat unscheduling. Skipping."));
                continue;
            }
        }
    }

    private String buildExceptionLogMessage(Exception exp,
                                            String errorMessagePrefix) {
        StringBuilder sb = new StringBuilder(errorMessagePrefix);
        sb.append(" ").append(exp.getMessage()).append(" [").append(exp.getClass().getSimpleName()).append("]\n")
                    .append(exp);
        return sb.toString();
    }

    private AgentRemoteValue buildPushRuntimeDiscoveryConfigArgs(ConfigurationInfo config,
                                                                 int typeInfo,
                                                                 String resourceKind)
        throws ResourceRepositoryException {
        Map<String, String> configuration =
                    resourceRepositoryService.getResourceAndAncestorsConfiguration(config.getResourceKey());

        AgentRemoteValue args =
                    AICommandsUtils.createArgForRuntimeDiscoveryConfig(
                                typeInfo,
                                config.getResourceInternalId(),
                                resourceKind,
                                resourceKind,
                                new ConfigResponse(configuration));
        return args;
    }

    private boolean isSyncMode() {
        return agentDaemon.getSyncModeManager().isSyncMode();
    }

    private void triggerScheduleMeasurementsCommand(ConfigurationInfo configInfo)
        throws AgentRunningException,
        PluginException, ResourceRepositoryException, SchedulingException, AgentRemoteException {
        TypeInfo typeInfo = configInfo.getTypeInfo();
        List<MergedSchedulingInfo> dsns = createDSNs(configInfo);
        ScheduleMeasurements_args commandArg = buildScheduleMeasurementCommand(configInfo.getResourceKey(),
                    configInfo.getResourceInternalId(), typeInfo, dsns);
        CommandInvokerUtil.triggerCommand(MeasurementCommandsAPI.command_scheduleMeasurements, commandArg);
    }

    private void triggerUnScheduleMeasurementsCommand(ConfigurationInfo configInfo)
        throws AgentRemoteException {

        UnscheduleMeasurements_args unschedCommandArg = buildUnscheduleMeasurementCommand(
                    configInfo.getResourceInternalId(), configInfo.getTypeInfo());
        CommandInvokerUtil.triggerCommand(MeasurementCommandsAPI.command_unscheduleMeasurements, unschedCommandArg);
    }

    private void triggerUnscheduleMeasurementsByIdCommand(int resourceInternalId)
        throws AgentRemoteException {
        UnscheduleMeasurementsById_args unschedCommandArg = new UnscheduleMeasurementsById_args(resourceInternalId);
        CommandInvokerUtil.triggerCommand(MeasurementCommandsAPI.command_unscheduleMeasurementsById, unschedCommandArg);
    }

    private ScheduleMeasurements_args buildScheduleMeasurementCommand(ResourceKey resourceKey,
                                                                      int resourceInternalId,
                                                                      TypeInfo typeInfo,
                                                                      List<MergedSchedulingInfo> mergedSchedulingInfos) {
        ScheduleMeasurements_args command = new ScheduleMeasurements_args();
        for (MergedSchedulingInfo mergedSchedulingInfo : mergedSchedulingInfos) {
            SchedulingInfo serverScheduling = mergedSchedulingInfo.getServerScheduling();
            long measurementId = convertToMeasurementId(resourceInternalId, serverScheduling.getMetricId());
            command.addMeasurement(mergedSchedulingInfo.getDsn(), serverScheduling.getPollingInterval(), measurementId,
                        measurementId, mergedSchedulingInfo.getCategory(), mergedSchedulingInfo.getUnits());
        }
        AppdefEntityID entityId = createAppDefEntityId(resourceInternalId, typeInfo);
        command.setSRN(new SRN(entityId, DEFAULT_REVISION_NUMBER));
        return command;
    }

    private UnscheduleMeasurements_args buildUnscheduleMeasurementCommand(int resourceInternalId,
                                                                          TypeInfo typeInfo) {
        UnscheduleMeasurements_args command = new UnscheduleMeasurements_args();
        AppdefEntityID entityId = createAppDefEntityId(resourceInternalId, typeInfo);
        command.addEntity(entityId);
        return command;
    }

    private List<MergedSchedulingInfo> createDSNs(ConfigurationInfo configInfo)
        throws AgentRunningException,
        PluginException, ResourceRepositoryException {

        if (CollectionUtils.isEmpty(configInfo.getScheduling())
                    && CollectionUtils.isEmpty(configInfo.getLwoScheduling())) {
            return Collections.emptyList();
        }
        List<MergedSchedulingInfo> mergedSchedulingInfos = new ArrayList<MergedSchedulingInfo>();
        Map<String, String> configMap =
                    resourceRepositoryService.getFullResourceConfiguration(configInfo.getResourceKey());

        // Handling Full Resource
        List<MergedSchedulingInfo> resourceDsn = createResourceDSN(configInfo, configMap);
        mergedSchedulingInfos.addAll(resourceDsn);

        // Handling LWO
        List<MergedSchedulingInfo> lwoDsn = createLwoDSN(configInfo, configMap);
        mergedSchedulingInfos.addAll(lwoDsn);
        return mergedSchedulingInfos;
    }

    private List<MergedSchedulingInfo> createResourceDSN(ConfigurationInfo configInfo,
                                                         Map<String, String> resourceConfigMap)
        throws AgentRunningException, PluginException {
        List<MergedSchedulingInfo> mergedSchedulingInfos = new ArrayList<MergedSchedulingInfo>();
        List<SchedulingInfo> schedulingInfos = configInfo.getScheduling();
        Map<String, MeasurementInfo> measurements = getMeasurements(configInfo.getResourceKind(),
                    configInfo.getTypeInfo());

        if (!CollectionUtils.isEmpty(measurements)) {
            ConfigResponse resourceConfigResponse = new ConfigResponse(resourceConfigMap);
            log.debug("Generating DSNs for " + configInfo.getResourceKind());
            for (SchedulingInfo schedInfo : schedulingInfos) {
                MergedSchedulingInfo mergedSchedulingInfo = getMergedSchedulingInfo(schedInfo, measurements,
                            resourceConfigResponse);
                if (mergedSchedulingInfo != null) {
                    mergedSchedulingInfos.add(mergedSchedulingInfo);
                }
            }
        }
        return mergedSchedulingInfos;
    }

    private List<MergedSchedulingInfo> createLwoDSN(ConfigurationInfo configInfo,
                                                    Map<String, String> resourceConfigMap)
        throws AgentRunningException, PluginException {
        List<MergedSchedulingInfo> mergedSchedulingInfos = new ArrayList<MergedSchedulingInfo>();
        Map<String, List<SchedulingInfo>> lwoScheduling = configInfo.getLwoScheduling();
        Map<String, Map<String, String>> lwoConfiguration = configInfo.getLwoConfiguration();

        for (Entry<String, List<SchedulingInfo>> entry : lwoScheduling.entrySet()) {
            // LWO Measurements
            String lwoKind = extractLwoResourceKind(entry.getKey());
            if (lwoKind == null) {
                log.error("Failed to extract LWO type from " + entry.getKey());
                continue;
            }
            TypeInfo lwoTypeInfo = agentDaemon.getTypeInfo(lwoKind, null);
            Map<String, MeasurementInfo> lwoMeasurements = getMeasurements(lwoKind, lwoTypeInfo);

            // LWO configResponse
            Map<String, String> lwoConfig = lwoConfiguration.get(entry.getKey());
            Map<String, String> fullConfiguration = new HashMap<String, String>();
            fullConfiguration.putAll(resourceConfigMap);
            if (lwoConfig != null) {
                fullConfiguration.putAll(lwoConfig);
            }
            ConfigResponse lwoConfigResponse = new ConfigResponse(fullConfiguration);

            // LWO DSN creation
            List<SchedulingInfo> schedlingInfos = entry.getValue();
            for (SchedulingInfo schedInfo : schedlingInfos) {
                MergedSchedulingInfo mergedSchedulingInfo = getMergedSchedulingInfo(schedInfo, lwoMeasurements,
                            lwoConfigResponse);
                if (mergedSchedulingInfo != null) {
                    mergedSchedulingInfos.add(mergedSchedulingInfo);
                }
            }
        }
        return mergedSchedulingInfos;
    }

    private MergedSchedulingInfo getMergedSchedulingInfo(SchedulingInfo schedInfo,
                                                         Map<String, MeasurementInfo> measurements,
                                                         ConfigResponse configResponse) {
        MergedSchedulingInfo mergedSchedulingInfo = null;
        try {
            MeasurementInfo measurementInfo = measurements.get(alignMetricName(schedInfo.getMetricName()));
            String template = measurementInfo.getTemplate();
            String dsn =
                        AgentDaemon.getMainInstance().getMeasurementPluginManager().translate(template, configResponse);
            mergedSchedulingInfo = new MergedSchedulingInfo();
            mergedSchedulingInfo.setServerScheduling(schedInfo);
            mergedSchedulingInfo.setDsn(dsn);
            mergedSchedulingInfo.setCategory(measurementInfo.getCategory());
            mergedSchedulingInfo.setUnits(measurementInfo.getUnits());
        } catch (Exception e) {
            log.error("Failed to create Measurement DSN for " + schedInfo.getMetricName());
        }
        return mergedSchedulingInfo;
    }

    /**
     * Returns the Resource kind for the lwo, null in case of an error The structure of the lwo is:
     * ResourceKind:InstanceName|MetricGroup|metric Note: as the InstanceName might contain semicolon, the first
     * occurrence of semicolon uses as a delimiter for the ResourceKind
     */
    private String extractLwoResourceKind(String lwo) {
        int delimiterLoc = lwo.indexOf(LWO_INSTANCE_DELIMITER);
        if (delimiterLoc == -1) { // no semicolon found
            return null;
        }
        return lwo.substring(0, delimiterLoc);
    }

    // Returns Map of metricKey to its MeasurementInfo Object
    private Map<String, MeasurementInfo> getMeasurements(String resourceKind,
                                                         TypeInfo typeInfo)
        throws AgentRunningException, PluginException {
        Map<String, MeasurementInfo> filteredMeasurements = new HashMap<String, MeasurementInfo>();
        MeasurementInfo[] measurements =
                    AgentDaemon.getMainInstance().getMeasurementPluginManager().getMeasurements(typeInfo);
        // Organize the templates to remove duplication
        for (int i = 0; i < measurements.length; i++) {
            String measurementKey = alignMetricName(measurements[i].getName());
            filteredMeasurements.put(measurementKey, measurements[i]);
        }
        return filteredMeasurements;
    }

    private AppdefEntityID createAppDefEntityId(int resourceId,
                                                TypeInfo typeInfo) {
        return new AppdefEntityID(typeInfo.getType() + ":" + resourceId);
    }

    private long convertToMeasurementId(int resId,
                                        int metricId) {
        return ((long) resId << 32) | (0xffffffffL & metricId);
    }

    private String alignMetricName(String metricName) {
        return metricName.replaceAll(" ", "").toLowerCase();
    }
}
