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

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.bizapp.shared.lather.AiSendRuntimeReport_args;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.upstream.inventory.AiSendRuntimeReportCommandData;
import com.vmware.epops.model.RawResource;
import com.vmware.epops.webapp.translators.lather.AiReportTranslator;
import com.vmware.epops.webapp.translators.lather.AiSendRuntimeReportTranslator;

public class AiSendRuntimeReportTranslatorTest extends TranslatorUtil {

    private AiSendRuntimeReportTranslator tested;
    private AiSendRuntimeReport_args mockedArgs;

    private CompositeRuntimeResourceReport mockedCompositeReport;
    private RuntimeResourceReport mockedRuntimeResourceReport1;
    private RuntimeResourceReport mockedRuntimeResourceReport2;

    @Before
    public void setUp()
        throws Exception {

        tested = new AiSendRuntimeReportTranslator();

        mockedArgs = Mockito.mock(AiSendRuntimeReport_args.class);
        mockedCompositeReport = Mockito.mock(CompositeRuntimeResourceReport.class);
        mockedRuntimeResourceReport1 = Mockito.mock(RuntimeResourceReport.class);
        mockedRuntimeResourceReport2 = Mockito.mock(RuntimeResourceReport.class);

        RuntimeResourceReport[] rrrArray = new RuntimeResourceReport[2];
        rrrArray[0] = mockedRuntimeResourceReport1;
        rrrArray[1] = mockedRuntimeResourceReport2;
        Mockito.when(mockedArgs.getReport()).thenReturn(mockedCompositeReport);
        Mockito.when(mockedCompositeReport.getServerReports()).thenReturn(rrrArray);

        AIPlatformValue platform1 = createPlatform(PLATFORM_1_TYPE, PLATFORM_1_NAME, PLATFORM_1_FQDN,
                    PLATFORM_1_DESCRIPTION);
        AIPlatformValue platform2 = createPlatform(PLATFORM_2_TYPE, PLATFORM_2_NAME, PLATFORM_2_FQDN,
                    PLATFORM_2_DESCRIPTION);
        AIPlatformValue platform3 = createPlatform(PLATFORM_3_TYPE, PLATFORM_3_NAME, PLATFORM_3_FQDN,
                    PLATFORM_3_DESCRIPTION);

        AIServerExtValue server1 = createServer(SERVER_1_TYPE, SERVER_1_NAME, SERVER_1_AID, SERVER_1_DESCRIPTION,
                    SERVER_1_INSTALL_PATH);
        AIServerExtValue server2 = createServer(SERVER_2_TYPE, SERVER_2_NAME, SERVER_2_AID, SERVER_2_DESCRIPTION,
                    SERVER_2_INSTALL_PATH);
        AIServerExtValue server3 = createServer(SERVER_3_TYPE, SERVER_3_NAME, SERVER_3_AID, SERVER_3_DESCRIPTION,
                    SERVER_3_INSTALL_PATH);

        AIServiceValue service1 = createService(SERVICE_1_TYPE, SERVICE_1_NAME);
        AIServiceValue service2 = createService(SERVICE_2_TYPE, SERVICE_2_NAME);
        AIServiceValue service3 = createService(SERVICE_3_TYPE, SERVICE_3_NAME);

        server1.addAIServiceValue(service1);
        server1.addAIServiceValue(service2);
        platform1.addAIServerValue(server1);

        server2.addAIServiceValue(service3);

        platform3.addAIServerValue(server2);
        platform3.addAIServerValue(server3);

        AIPlatformValue[] report1Platforms = new AIPlatformValue[2];
        report1Platforms[0] = platform1;
        report1Platforms[1] = platform2;

        AIPlatformValue[] report2Platforms = new AIPlatformValue[1];
        report2Platforms[0] = platform3;

        Mockito.when(mockedRuntimeResourceReport1.getAIPlatforms()).thenReturn(report1Platforms);
        Mockito.when(mockedRuntimeResourceReport2.getAIPlatforms()).thenReturn(report2Platforms);

    }

    @Test
    public void testTranslateRequest() {

        AiSendRuntimeReportCommandData result = (AiSendRuntimeReportCommandData)
                    tested.translateRequest(mockedArgs, AGENT_TOKEN);

        Assert.assertEquals("Agent token didn't match expectation", AGENT_TOKEN, result.getAgentToken());
        Assert.assertEquals("Command name didn't match expectation", AiSendRuntimeReportCommandData.COMMAND_NAME,
                    result.getCommandName());

        List<RawResource> resources = result.getRawResources();
        Assert.assertEquals("Resources list size didn't match expectation", 3, resources.size());

        int i = 0;
        for (RawResource platform : resources) {
            List<RawResource> servers = platform.getChildren();
            switch (platform.getResourceName()) {
                case PLATFORM_1_NAME:
                    i++;
                    checkPlatformValues(platform, PLATFORM_1_NAME, PLATFORM_1_FQDN, PLATFORM_1_TYPE,
                                PLATFORM_1_DESCRIPTION, 1);
                    checkServerValues(servers.get(0), SERVER_1_NAME, SERVER_1_AID, SERVER_1_TYPE,
                                SERVER_1_INSTALL_PATH, SERVER_1_DESCRIPTION, 2);
                    verifyServices(servers.get(0).getChildren());
                    break;

                case PLATFORM_2_NAME:
                    i++;
                    Assert.assertEquals("Servers list size didn't match expectation", 0, servers.size());
                    break;

                case PLATFORM_3_NAME:
                    i++;
                    Assert.assertEquals("Servers list size didn't match expectation", 2, servers.size());
                    verifyServers(servers);
                    break;

            }
        }
        Assert.assertEquals("Platforms were not as expected", 3, i);
    }

    private void verifyServers(List<RawResource> servers) {
        int i = 0;
        for (RawResource server : servers) {
            List<RawResource> services = server.getChildren();
            switch (server.getResourceName()) {
                case SERVER_2_NAME:
                    i++;
                    checkServerValues(server, SERVER_2_NAME, SERVER_2_AID, SERVER_2_TYPE, SERVER_2_INSTALL_PATH,
                                SERVER_2_DESCRIPTION, 1);
                    checkServiceValues(services.get(0), SERVICE_3_NAME, SERVICE_3_TYPE);
                    break;

                case SERVER_3_NAME:
                    i++;
                    checkServerValues(server, SERVER_3_NAME, SERVER_3_AID, SERVER_3_TYPE, SERVER_3_INSTALL_PATH,
                                SERVER_3_DESCRIPTION, 0);
                    break;

            }
        }
        Assert.assertEquals("Servers were not as expected", 2, i);
    }

    private void verifyServices(List<RawResource> services) {
        int i = 0;
        for (RawResource service : services) {
            switch (service.getResourceName()) {
                case SERVICE_1_NAME:
                    i++;
                    checkServiceValues(service, SERVICE_1_NAME, SERVICE_1_TYPE);
                    break;

                case SERVICE_2_NAME:
                    i++;
                    checkServiceValues(service, SERVICE_2_NAME, SERVICE_2_TYPE);
                    break;

            }
        }
        Assert.assertEquals("Services were not as expected", 2, i);
    }

    @Test
    public void testSyncReport() {

        Map<String, String> vals = new HashMap<>(1);
        vals.put(AiReportTranslator.SYNC_SCAN, "true");
        Mockito.when(mockedArgs.getStringVals()).thenReturn(vals);
        AiSendRuntimeReportCommandData result = (AiSendRuntimeReportCommandData)
                    tested.translateRequest(mockedArgs, AGENT_TOKEN);

        List<RawResource> resources = result.getRawResources();
        for (RawResource platform : resources) {
            Assert.assertTrue("Resource sync value is not as expected", platform.isSync());
            for (RawResource server : platform.getChildren()) {
                Assert.assertTrue("Resource sync value is not as expected", server.isSync());
                for (RawResource service : server.getChildren()) {
                    Assert.assertTrue("Resource sync value is not as expected", service.isSync());
                }
            }
        }
    }

    @Test
    public void testTranslateRequestNullArgs() {
        AgentCommandData actual = tested.translateRequest(null, AGENT_TOKEN);
        Assert.assertNull("Expected a null value for null input", actual);
    }

    @Test
    public void testTranslateRequestNullAgentToken() {
        AgentCommandData translated = tested.translateRequest(mockedArgs, null);
        Assert.assertNull("Expected null", translated);
    }

    @Test
    public void testTranslateRequestNullCompositeReport() {
        Mockito.when(mockedArgs.getReport()).thenReturn(null);
        AiSendRuntimeReportCommandData result = (AiSendRuntimeReportCommandData)
                    tested.translateRequest(mockedArgs, AGENT_TOKEN);
        Assert.assertTrue("Expected no resources", result.getRawResources().isEmpty());
    }

    @Test
    public void testTranslateRequestNullReports() {
        Mockito.when(mockedArgs.getReport().getServerReports()).thenReturn(null);
        AiSendRuntimeReportCommandData result = (AiSendRuntimeReportCommandData)
                    tested.translateRequest(mockedArgs, AGENT_TOKEN);
        Assert.assertTrue("Expected no resources", result.getRawResources().isEmpty());
    }

    @Test
    public void testTranslateRequestNullPlatforms() {
        Mockito.when(mockedRuntimeResourceReport1.getAIPlatforms()).thenReturn(null);
        AiSendRuntimeReportCommandData result = (AiSendRuntimeReportCommandData)
                    tested.translateRequest(mockedArgs, AGENT_TOKEN);
        // the first report returns null for getPlatforms -> we expect to have only 1 platform instead of 3
        Assert.assertEquals("Resources size didn't match expectation", 1, result.getRawResources().size());
    }

}
