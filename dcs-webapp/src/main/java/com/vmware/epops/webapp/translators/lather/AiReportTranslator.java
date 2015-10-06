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

package com.vmware.epops.webapp.translators.lather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.lather.LatherValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.slf4j.Logger;

import com.vmware.epops.model.RawResource;
import com.vmware.epops.model.config.NetworkConfig;
import com.vmware.epops.plugin.Constants;
import com.vmware.epops.plugin.converter.NameMappingUtil;
import com.vmware.epops.util.converter.NetworkConfigConverter;

public abstract class AiReportTranslator implements AgentVerifiedLatherCommandTranslator {

    public static final String SYNC_SCAN = "syncScan";

    protected abstract Logger getLogger();

    @SuppressWarnings("unchecked")
    protected Map<String, String> extractProperties(byte[] config) {
        Map<String, String> properties = new HashMap<>();
        ConfigResponse configResponse = decodeConfig(config);
        if (configResponse != null) {
            properties = configResponse.getConfig();
        }
        return properties;
    }

    protected ConfigResponse decodeConfig(byte[] config) {
        if (config == null) {
            return null;
        }
        try {
            return ConfigResponse.decode(config);
        } catch (EncodingException e) {
            getLogger().warn("Failed to read some of the properties received");
            return null;
        }
    }

    protected boolean isSyncReport(LatherValue latherValue) {
        String syncReportStr = (String) latherValue.getStringVals().get(SYNC_SCAN);
        boolean syncReport = false;
        if (syncReportStr != null) {
            syncReport = Boolean.parseBoolean(syncReportStr);
        }
        return syncReport;
    }

    protected RawResource createPlatformResource(ReportParams reportParams,
                                                 AIPlatformValue platform,
                                                 boolean createChildren) {
        String resourceType = NameMappingUtil.convertResourceTypeName(platform.getPlatformTypeName());
        String resourceName = platform.getName();
        String fqdn = platform.getFqdn();
        String agentToken = reportParams.getAgentToken();

        RawResource resource = new RawResource(StringUtils.isEmpty(resourceName) ? fqdn : resourceName,
                    agentToken, resourceType, platform.getId());
        resource.setAgentToken(agentToken);
        resource.setSync(reportParams.isSync());

        Map<String, String> configProps = extractProperties(platform.getProductConfig());
        resource.setConfigProperties(configProps);

        Map<String, String> discoveredProps = extractProperties(platform.getCustomProperties());
        addPlatformAdditionalProperties(platform, discoveredProps);
        resource.setDiscoveredProperties(discoveredProps);
        if (createChildren) {
            resource.setChildren(getServerResources(reportParams, platform.getAIServerValues()));
        }
        getLogger().debug("Creating platform Raw Resource with internal id {}", resource.getInternalId());
        return resource;
    }

    protected List<RawResource> getServerResources(ReportParams reportParams,
                                                   AIServerValue[] servers) {
        List<RawResource> serverResources = new ArrayList<RawResource>();
        for (AIServerValue server : servers) {
            serverResources.add(createServerResource(reportParams, (AIServerExtValue) server, true));
        }
        return serverResources;
    }

    protected RawResource createServerResource(ReportParams reportParams,
                                               AIServerExtValue server,
                                               boolean createChildren) {
        RawResource resource = new RawResource(server.getName(), server.getAutoinventoryIdentifier(),
                    server.getServerTypeName(), server.getId());
        resource.setAgentToken(reportParams.getAgentToken());
        resource.setSync(reportParams.isSync());

        Map<String, String> discoveredProps = extractProperties(server.getCustomProperties());
        addServerAdditionalProperties(server, discoveredProps);
        resource.setDiscoveredProperties(discoveredProps);

        Map<String, String> configProps = extractProperties(server.getProductConfig());
        configProps.putAll(extractProperties(server.getMeasurementConfig()));
        addServerAdditionalConfiguration(server, configProps);
        resource.setConfigProperties(configProps);

        if (createChildren) {
            resource.setChildren(getServiceResources(reportParams, server.getAIServiceValues()));
        }
        getLogger().debug("Creating server Raw Resource with internal id {}", resource.getInternalId());
        return resource;
    }

    private List<RawResource> getServiceResources(ReportParams reportParams,
                                                  AIServiceValue[] services) {
        List<RawResource> serviceResources = new ArrayList<RawResource>();
        if (services != null) {
            for (AIServiceValue service : services) {
                serviceResources.add(createServiceResource(reportParams, service));
            }
        }
        return serviceResources;
    }

    private RawResource createServiceResource(ReportParams reportParams,
                                              AIServiceValue service) {
        RawResource resource = new RawResource(service.getName(), service.getName(),
                    service.getServiceTypeName(), service.getId());
        resource.setAgentToken(reportParams.getAgentToken());
        resource.setSync(reportParams.isSync());

        Map<String, String> configProps = extractProperties(service.getProductConfig());
        configProps.putAll(extractProperties(service.getMeasurementConfig()));
        resource.setConfigProperties(configProps);

        Map<String, String> discoveredProps = extractProperties(service.getCustomProperties());
        // No hard coded properties for service
        resource.setDiscoveredProperties(discoveredProps);
        getLogger().debug("Creating service Raw Resource with internal id {}", resource.getInternalId());
        return resource;
    }

    private void addPlatformAdditionalProperties(AIPlatformValue platform,
                                                 Map<String, String> discoveredProps) {
        // Add the platform hard coded properties only if they are not null (can happen in runtime report)
        String name = StringUtils.isEmpty(platform.getName()) ? platform.getFqdn() : platform.getName();
        if (name != null) {
            discoveredProps.put(Constants.NAME, name);
        }
        if (platform.getFqdn() != null) {
            discoveredProps.put(Constants.FQDN, platform.getFqdn());
        }
        if (platform.getPlatformTypeName() != null) {
            discoveredProps.put(Constants.TYPE, NameMappingUtil.convertResourceTypeName(
                        platform.getPlatformTypeName()));
        }
        if (platform.getDescription() != null) {
            discoveredProps.put(Constants.DESCRIPTION, platform.getDescription());
        }
        List<NetworkConfig> iPValues = getIPValues(platform.getAIIpValues());
        if (iPValues != null) {
            discoveredProps.put(Constants.NETWORK_NAME, NetworkConfigConverter.formatList(iPValues));
        }
    }

    private void addServerAdditionalProperties(AIServerExtValue server,
                                               Map<String, String> discoveredProps) {
        // Add the server hard coded properties only if they are not null (can happen in runtime report)
        if (server.getName() != null) {
            discoveredProps.put(Constants.NAME, server.getName());
        }
        if (server.getDescription() != null) {
            discoveredProps.put(Constants.DESCRIPTION, server.getDescription());
        }
    }

    private void addServerAdditionalConfiguration(AIServerExtValue server,
                                                  Map<String, String> configProps) {
        // Add the server hard coded identifier only if they are not null (can happen in runtime report)
        if (server.getInstallPath() != null) {
            configProps.put(Constants.INSTALL_PATH_NAME, server.getInstallPath());
        }
    }

    private List<NetworkConfig> getIPValues(AIIpValue[] originalAIIpValues) {
        if (ArrayUtils.isEmpty(originalAIIpValues)) {
            return null;
        }
        List<NetworkConfig> ipValues = new ArrayList<>(originalAIIpValues.length);
        for (AIIpValue originalAIIpValue : originalAIIpValues) {
            ipValues.add(new NetworkConfig(originalAIIpValue.getAddress(),
                        originalAIIpValue.getNetmask(), originalAIIpValue.getMACAddress()));
        }
        return ipValues;
    }

}
