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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vmware.epops.webapp.translators.lather.registration.LatherRegistrationCommandTranslator;
import com.vmware.epops.webapp.translators.lather.registration.LatherRegistrationCommandTranslatorFactory;

public class LatherRegistrationCommandTranslatorFactoryTest {

    private IMocksControl mocksControl;
    private LatherRegistrationCommandTranslator mockLatherRegistrationCommandTranslator;
    private LatherRegistrationCommandTranslatorFactory tested;

    @Before
    public void setUp() {
        mocksControl = EasyMock.createControl();
        mockLatherRegistrationCommandTranslator = mocksControl.createMock(LatherRegistrationCommandTranslator.class);
        tested = LatherRegistrationCommandTranslatorFactory.INSTANCE;
    }

    @Test
    public void testGetExistingTranslator() {
        final String mockCommandName = "mock-command";
        tested.registerTranslator(mockCommandName, mockLatherRegistrationCommandTranslator);
        LatherRegistrationCommandTranslator actual = tested.getTranslator(mockCommandName);
        Assert.assertEquals("The actual translator is different than the expected",
                    mockLatherRegistrationCommandTranslator, actual);
    }

    @Test
    public void testGetNonExistingTranslator() {
        LatherRegistrationCommandTranslator actual = tested.getTranslator("non-existing-commnd");
        Assert.assertNull("Expected to get no translator for a non existing command", actual);
    }

}
