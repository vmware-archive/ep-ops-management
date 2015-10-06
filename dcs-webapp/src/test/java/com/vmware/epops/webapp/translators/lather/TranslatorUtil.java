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

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Assert;

import com.vmware.epops.model.RawResource;
import com.vmware.epops.plugin.Constants;

public class TranslatorUtil {

    protected static final String AGENT_TOKEN = "agent-token";

    protected static final String PLATFORM_1_DESCRIPTION = "platform-1-description";
    protected static final String PLATFORM_1_TYPE = "platform-1-type";
    protected static final String PLATFORM_1_FQDN = "platform-1-fqdn";
    protected static final String PLATFORM_1_NAME = "platform-1-name";

    protected static final String PLATFORM_2_DESCRIPTION = "platform-2-description";
    protected static final String PLATFORM_2_TYPE = "platform-2-type";
    protected static final String PLATFORM_2_FQDN = "platform-2-fqdn";
    protected static final String PLATFORM_2_NAME = "platform-2-name";

    protected static final String PLATFORM_3_DESCRIPTION = "platform-3-description";
    protected static final String PLATFORM_3_TYPE = "platform-3-type";
    protected static final String PLATFORM_3_FQDN = "platform-3-fqdn";
    protected static final String PLATFORM_3_NAME = "platform-3-name";

    protected static final String SERVER_1_DESCRIPTION = "server-1-description";
    protected static final String SERVER_1_TYPE = "server-1-type";
    protected static final String SERVER_1_AID = "server-1-ai-id";
    protected static final String SERVER_1_NAME = "server-1-name";
    protected static final String SERVER_1_INSTALL_PATH = "server-1-install-path";

    protected static final String SERVER_2_DESCRIPTION = "server-2-description";
    protected static final String SERVER_2_TYPE = "server-2-type";
    protected static final String SERVER_2_AID = "server-2-ai-id";
    protected static final String SERVER_2_NAME = "server-2-name";
    protected static final String SERVER_2_INSTALL_PATH = "server-2-install-path";

    protected static final String SERVER_3_DESCRIPTION = "server-3-description";
    protected static final String SERVER_3_TYPE = "server-3-type";
    protected static final String SERVER_3_AID = "server-3-ai-id";
    protected static final String SERVER_3_NAME = "server-3-name";
    protected static final String SERVER_3_INSTALL_PATH = "server-3-install-path";

    protected static final String SERVICE_1_TYPE = "service-1-type";
    protected static final String SERVICE_1_NAME = "service-1-name";

    protected static final String SERVICE_2_TYPE = "service-2-type";
    protected static final String SERVICE_2_NAME = "service-2-name";

    protected static final String SERVICE_3_TYPE = "service-3-type";
    protected static final String SERVICE_3_NAME = "service-3-name";

    protected static final String ADDRESS_1 = "address-1";
    protected static final String ADDRESS_2 = "address-2";
    protected static final String MAC_1 = "mac-1";
    protected static final String MAC_2 = "mac-2";
    protected static final String NETMASK_1 = "netmask-1";
    protected static final String NETMASK_2 = "netmask-2";

    protected static final Map<String, String> customeProps;
    static {
        customeProps = new HashMap<>();
        customeProps.put("custome1", "value1");
        customeProps.put("custome2", "value2");
    }

    protected static final Map<String, String> productConfig;
    static {
        productConfig = new HashMap<>();
        productConfig.put("product1", "value1");
        productConfig.put("product2", "value2");
    }

    protected static final Map<String, String> measurementConfig;
    static {
        measurementConfig = new HashMap<>();
        measurementConfig.put("measurement1", "value1");
        measurementConfig.put("measurement2", "value2");
    }

    protected AIPlatformValue createPlatform(String type,
                                             String name,
                                             String fqdn,
                                             String description) {
        AIPlatformValue platform = new AIPlatformValue();
        platform.setPlatformTypeName(type);
        platform.setFqdn(fqdn);
        platform.setName(name);
        platform.setDescription(description);
        return platform;
    }

    protected AIServerExtValue createServer(String type,
                                            String name,
                                            String aiId,
                                            String description,
                                            String installPath) {
        AIServerExtValue server = new AIServerExtValue();
        server.setServerTypeName(type);
        server.setAutoinventoryIdentifier(aiId);
        server.setName(name);
        server.setDescription(description);
        server.setInstallPath(installPath);
        return server;
    }

    protected AIServiceValue createService(String type,
                                           String name)
        throws EncodingException {
        AIServiceValue service = new AIServiceValue();
        service.setName(name);
        service.setServiceTypeName(type);
        service.setCustomProperties(ConfigResponse.safeEncode(new ConfigResponse(customeProps)));
        service.setProductConfig(ConfigResponse.safeEncode(new ConfigResponse(productConfig)));
        service.setMeasurementConfig(ConfigResponse.safeEncode(new ConfigResponse(measurementConfig)));
        return service;
    }

    protected void checkPlatformValues(RawResource platform,
                                       String expectedName,
                                       String expectedFqdn,
                                       String expectedType,
                                       String expectedDescription,
                                       int expectedServices) {
        Assert.assertEquals("Agent token is not as expceted", AGENT_TOKEN, platform.getAgentToken());
        Assert.assertFalse("Platform sync value is not as expected", platform.isSync());
        Assert.assertEquals("Platform name is not as expceted", expectedName, platform.getResourceName());
        Assert.assertEquals("AI ID is not as expceted", AGENT_TOKEN, platform.getRawIdentifier());
        Assert.assertEquals("Platform type is not as expceted", expectedType, platform.getResourceType());
        Assert.assertEquals("Platform internal id is not as expceted", null, platform.getInternalId());
        Assert.assertEquals("Servers list size didn't match expectation", expectedServices,
                    platform.getChildren().size());

        Map<String, String> expected = new HashMap<>();
        expected.put(Constants.NAME, expectedName);
        expected.put(Constants.FQDN, expectedFqdn);
        expected.put(Constants.TYPE, expectedType);
        expected.put(Constants.DESCRIPTION, expectedDescription);

        checkAdditonalProperties(expected, platform.getDiscoveredProperties());

    }

    protected void checkServerValues(RawResource server,
                                     String expectedName,
                                     String expectedAID,
                                     String expectedType,
                                     String expectedInstallPath,
                                     String expectedDescription,
                                     int expectedServices) {
        Assert.assertEquals("Agent token is not as expceted", AGENT_TOKEN, server.getAgentToken());
        Assert.assertFalse("Server sync value is not as expected", server.isSync());
        Assert.assertEquals("Server name is not as expceted", expectedName, server.getResourceName());
        Assert.assertEquals("AI ID is not as expceted", expectedAID, server.getRawIdentifier());
        Assert.assertEquals("Server type is not as expceted", expectedType, server.getResourceType());
        Assert.assertEquals("Server internal id is not as expceted", null, server.getInternalId());
        Assert.assertEquals("Services list size didn't match expectation", expectedServices,
                    server.getChildren().size());

        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put(Constants.NAME, expectedName);
        expectedProperties.put(Constants.DESCRIPTION, expectedDescription);

        checkAdditonalProperties(expectedProperties, server.getDiscoveredProperties());

        Map<String, String> expectedIdentifiers = new HashMap<>();
        expectedIdentifiers.put(Constants.INSTALL_PATH_NAME, expectedInstallPath);

        checkAdditonalProperties(expectedIdentifiers, server.getConfigProperties());
    }

    protected void checkServiceValues(RawResource service,
                                      String expectedName,
                                      String expectedType) {
        Assert.assertEquals("Agent token is not as expceted", AGENT_TOKEN, service.getAgentToken());
        Assert.assertFalse("Service sync value is not as expected", service.isSync());
        Assert.assertEquals("Service name is not as expceted", expectedName, service.getResourceName());
        Assert.assertEquals("AI ID is not as expceted", expectedName, service.getRawIdentifier());
        Assert.assertEquals("Service type is not as expceted", expectedType, service.getResourceType());
        Assert.assertEquals("Service internal id is not as expceted", null, service.getInternalId());
        Assert.assertEquals("Child list size didn't match expectation", 0, service.getChildren().size());

        Map<String, String> configProps = productConfig;
        configProps.putAll(measurementConfig);

        checkProperties(configProps, service.getConfigProperties(), true);
        checkProperties(customeProps, service.getDiscoveredProperties(), true);

    }

    protected void checkAdditonalProperties(Map<String, String> expected,
                                            Map<String, String> actual) {
        checkProperties(expected, actual, false);
    }

    protected void checkProperties(Map<String, String> expected,
                                   Map<String, String> actual,
                                   boolean checkSize) {
        if (checkSize) {
            Assert.assertEquals(expected.size(), actual.size());
        }
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Assert.assertEquals(entry.getValue(), actual.get(entry.getKey()));
        }
    }

}
