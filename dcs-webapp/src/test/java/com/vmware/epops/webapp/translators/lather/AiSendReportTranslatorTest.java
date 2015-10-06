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
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.ScanMethodState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.upstream.inventory.AiSendReportCommandData;
import com.vmware.epops.model.RawResource;
import com.vmware.epops.plugin.Constants;
import com.vmware.epops.webapp.translators.lather.AiReportTranslator;
import com.vmware.epops.webapp.translators.lather.AiSendReportTranslator;

public class AiSendReportTranslatorTest extends TranslatorUtil {

    private AiSendReportTranslator tested;
    private AiSendReport_args mockedArgs;
    private ScanStateCore core;

    @Before
    public void setUp()
        throws Exception {
        tested = new AiSendReportTranslator();

        mockedArgs = Mockito.mock(AiSendReport_args.class);

        // Mock core
        core = Mockito.mock(ScanStateCore.class);
        Mockito.when(mockedArgs.getCore()).thenReturn(core);

        // Mock states
        ScanMethodState[] mockedStates = new ScanMethodState[2];
        ScanMethodState mockedState = Mockito.mock(ScanMethodState.class);
        ScanMethodState mockedEmptyState = Mockito.mock(ScanMethodState.class);
        mockedStates[0] = mockedState;
        mockedStates[1] = mockedEmptyState;
        Mockito.when(core.getScanMethodStates()).thenReturn(mockedStates);

        AIIpValue iPValueOne = new AIIpValue();
        AIIpValue iPValueTwo = new AIIpValue();

        iPValueOne.setAddress(ADDRESS_1);
        iPValueOne.setMACAddress(MAC_1);
        iPValueOne.setNetmask(NETMASK_1);
        iPValueTwo.setAddress(ADDRESS_2);
        iPValueTwo.setMACAddress(MAC_2);
        iPValueTwo.setNetmask(NETMASK_2);

        AIPlatformValue realPlatform =
                    createPlatform(PLATFORM_1_TYPE, PLATFORM_1_NAME, PLATFORM_1_FQDN, PLATFORM_1_DESCRIPTION);
        realPlatform.addAIIpValue(iPValueOne);
        realPlatform.addAIIpValue(iPValueTwo);
        realPlatform.setCustomProperties(new ConfigResponse(customeProps).encode());

        // (Real) server values (can't use mock because of the casting from AIServerValue to AIServerExtValue)
        AIServerValue[] realServers = new AIServerValue[2];
        AIServerValue realServerOne =
                    createServer(SERVER_1_TYPE, SERVER_1_NAME, SERVER_1_AID, SERVER_1_DESCRIPTION,
                                SERVER_1_INSTALL_PATH);
        realServerOne.setCustomProperties(new ConfigResponse(customeProps).encode());

        AIServerValue realServerTwo =
                    createServer(SERVER_2_TYPE, SERVER_2_NAME, SERVER_2_AID, SERVER_2_DESCRIPTION,
                                SERVER_2_INSTALL_PATH);
        realServerTwo.setCustomProperties(new ConfigResponse(customeProps).encode());

        realServers[0] = realServerOne;
        realServers[1] = realServerTwo;

        Mockito.when(core.getPlatform()).thenReturn(realPlatform);
        Mockito.when(mockedState.getServers()).thenReturn(realServers);
        Mockito.when(mockedEmptyState.getServers()).thenReturn(null);

    }

    @Test
    public void testTranslateRequest()
        throws EncodingException {

        AgentCommandData translated = tested.translateRequest(mockedArgs, AGENT_TOKEN);

        Assert.assertTrue("Translated Request doesn't match expected",
                    translated instanceof AiSendReportCommandData);

        AiSendReportCommandData aiReportCommandData = (AiSendReportCommandData) translated;
        List<RawResource> resources = aiReportCommandData.getRawResources();
        Assert.assertNotNull(resources);
        Assert.assertEquals(1, resources.size());
        RawResource resource = resources.get(0);

        checkPlatformValues(resource, PLATFORM_1_NAME, PLATFORM_1_FQDN, PLATFORM_1_TYPE, PLATFORM_1_DESCRIPTION, 2);
        checkIPValues(resource);

        List<RawResource> servers = resource.getChildren();

        RawResource serverOne = servers.get(0);
        RawResource serverTwo = servers.get(1);
        checkServerValues(serverOne, SERVER_1_NAME, SERVER_1_AID, SERVER_1_TYPE, SERVER_1_INSTALL_PATH,
                    SERVER_1_DESCRIPTION, 0);
        checkServerValues(serverTwo, SERVER_2_NAME, SERVER_2_AID, SERVER_2_TYPE, SERVER_2_INSTALL_PATH,
                    SERVER_2_DESCRIPTION, 0);

        checkProperties(customeProps, serverOne.getDiscoveredProperties(), false);
    }

    private void checkIPValues(RawResource resource) {
        String networkConfig = resource.getDiscoveredProperties().get(Constants.NETWORK_NAME);
        Assert.assertTrue("IP values doesn't match", networkConfig.contains(ADDRESS_1));
        Assert.assertTrue("IP values doesn't match", networkConfig.contains(NETMASK_1));
        Assert.assertTrue("IP values doesn't match", networkConfig.contains(MAC_1));
        Assert.assertTrue("IP values doesn't match", networkConfig.contains(ADDRESS_2));
        Assert.assertTrue("IP values doesn't match", networkConfig.contains(NETMASK_2));
        Assert.assertTrue("IP values doesn't match", networkConfig.contains(MAC_2));
    }

    @Test
    public void testSyncReport()
        throws EncodingException {

        Map<String, String> vals = new HashMap<>(1);
        vals.put(AiReportTranslator.SYNC_SCAN, "true");
        Mockito.when(mockedArgs.getStringVals()).thenReturn(vals);
        AgentCommandData translated = tested.translateRequest(mockedArgs, AGENT_TOKEN);

        Assert.assertTrue("Translated Request doesn't match expected",
                    translated instanceof AiSendReportCommandData);

        AiSendReportCommandData aiReportCommandData = (AiSendReportCommandData) translated;
        List<RawResource> resources = aiReportCommandData.getRawResources();
        Assert.assertNotNull(resources);
        Assert.assertEquals(1, resources.size());
        RawResource resource = resources.get(0);

        // Check sync value
        Assert.assertTrue("Resource sync value is not as expected", resource.isSync());
        for (RawResource server : resource.getChildren()) {
            Assert.assertTrue("Resource sync value is not as expected", server.isSync());
        }
    }

    @Test
    public void testTranslateRequestNullCore() {
        Mockito.when(mockedArgs.getCore()).thenReturn(null);
        Assert.assertNull("Translated request should be null given null core",
                    tested.translateRequest(mockedArgs, AGENT_TOKEN));
    }

    @Test
    public void testTranslateRequestNullPlatform() {
        Mockito.when(core.getPlatform()).thenReturn(null);
        Mockito.when(mockedArgs.getCore()).thenReturn(core);
        Assert.assertNull("Translated request should be null given null platform",
                    tested.translateRequest(mockedArgs, AGENT_TOKEN));
    }

    @Test
    public void testTranslateRequestNullStates() {
        Mockito.when(core.getScanMethodStates()).thenReturn(null);
        Mockito.when(mockedArgs.getCore()).thenReturn(core);
        Assert.assertNull("Translated request should be null given null states",
                    tested.translateRequest(mockedArgs, AGENT_TOKEN));
    }

    @Test
    public void testTranslateRequestNull() {
        AgentCommandData translated = tested.translateRequest(null, AGENT_TOKEN);
        Assert.assertNull("Expected null", translated);
    }

    @Test
    public void testTranslateRequestNullAgentToken()
        throws EncodingException {
        AgentCommandData translated = tested.translateRequest(mockedArgs, null);
        Assert.assertNull("Expected null", translated);
    }

}
