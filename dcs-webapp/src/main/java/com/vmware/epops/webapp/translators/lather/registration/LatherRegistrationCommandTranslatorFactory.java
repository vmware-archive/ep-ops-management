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

import java.util.HashMap;
import java.util.Map;

public enum LatherRegistrationCommandTranslatorFactory {
    INSTANCE; // a single-element enum type is the best way to implement a
    // singleton according to Effective Java 2ed.

    private final Map<String, LatherRegistrationCommandTranslator> translatorsMap =
                new HashMap<String, LatherRegistrationCommandTranslator>();

    public void registerTranslator(String commandName,
                                   LatherRegistrationCommandTranslator translator) {
        translatorsMap.put(commandName, translator);
    }

    public LatherRegistrationCommandTranslator getTranslator(String commandName) {
        return translatorsMap.get(commandName);
    }
}
