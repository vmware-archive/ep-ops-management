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

package com.vmware.epops.plugin.converter;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.InvalidOptionValueException;

public class PropertyConfigOption extends ConfigOption {

    private static final long serialVersionUID = 1L;
    private ConfigOption configOption;

    @Override
    public void checkOptionIsValid(String option)
        throws InvalidOptionValueException {
        getConfigOption().checkOptionIsValid(option);
    }

    public ConfigOption getConfigOption() {
        return configOption;
    }

    public void setConfigOption(ConfigOption configOption) {
        this.configOption = configOption;
    }

    @Override
    public boolean equals(final Object obj) {
        return getConfigOption().equals(obj);
    }

    @Override
    public int hashCode() {
        return getConfigOption().hashCode();
    }

    @Override
    public String toString() {
        return getConfigOption().toString();
    }

}
