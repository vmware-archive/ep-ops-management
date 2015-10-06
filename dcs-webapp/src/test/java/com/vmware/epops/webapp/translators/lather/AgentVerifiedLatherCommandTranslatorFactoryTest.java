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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vmware.epops.webapp.translators.lather.AgentVerifiedLatherCommandTranslator;
import com.vmware.epops.webapp.translators.lather.AgentVerifiedLatherCommandTranslatorFactory;
import com.vmware.epops.webapp.translators.lather.LatherCommandTranslator;

public class AgentVerifiedLatherCommandTranslatorFactoryTest {

    private IMocksControl mocksControl;
    private AgentVerifiedLatherCommandTranslator mockAgentVerifiedLatherCommandTranslator;
    private AgentVerifiedLatherCommandTranslatorFactory tested;

    @Before
    public void setUp() {
        mocksControl = EasyMock.createControl();
        mockAgentVerifiedLatherCommandTranslator = mocksControl.createMock(AgentVerifiedLatherCommandTranslator.class);
        tested = AgentVerifiedLatherCommandTranslatorFactory.INSTANCE;
    }

    @Test
    public void testGetExistingTranslator() {
        final String mockCommandName = "mock-command";
        tested.registerTranslator(mockCommandName, mockAgentVerifiedLatherCommandTranslator);
        LatherCommandTranslator actual = tested.getTranslator(mockCommandName);
        Assert.assertEquals("The actual translator is different than the expected",
                    mockAgentVerifiedLatherCommandTranslator, actual);
    }

    @Test
    public void testGetNonExistingTranslator() {
        LatherCommandTranslator actual = tested.getTranslator("non-existing-commnd");
        Assert.assertNull("Expected to get no translator for a non existing command", actual);
    }

}
