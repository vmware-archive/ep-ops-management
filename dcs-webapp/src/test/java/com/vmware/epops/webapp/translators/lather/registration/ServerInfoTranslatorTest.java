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

package com.vmware.epops.webapp.translators.lather.registration;

import org.hyperic.hq.bizapp.shared.lather.ServerInfo_result;
import org.hyperic.lather.LatherValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.upstream.registration.ServerInfoCommandResponse;

/**
 * Unit test for {@link ServerInfoTranslator}
 * 
 * @author yyogev
 */
public class ServerInfoTranslatorTest {

    private ServerInfoTranslator tested;

    @Before
    public void setUp()
        throws Exception {
        tested = new ServerInfoTranslator();
    }

    /**
     * Test method for {@link ServerInfoTranslator#translateRequest(LatherValue)}
     */
    @Test
    public void testTranslateRequest() {
        LatherValue mockedLatherValue = Mockito.mock(LatherValue.class);
        AgentCommandData translated = tested.translateRequest(mockedLatherValue);
        String actual = translated.getCommandName();
        String expected = "serverInfo";
        Assert.assertEquals("Returned command name wasn't as expected", expected, actual);
    }

    @Test
    public void testTranslateResponse() {
        ServerInfoCommandResponse mockedResponse = Mockito.mock(ServerInfoCommandResponse.class);
        LatherValue response = tested.translateResponse(mockedResponse);
        Assert.assertTrue("Returned type wasn't as expected", response instanceof ServerInfo_result);
    }

}
