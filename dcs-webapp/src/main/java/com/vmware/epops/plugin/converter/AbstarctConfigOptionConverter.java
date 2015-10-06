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
import org.springframework.core.convert.converter.Converter;

import com.vmware.epops.plugin.model.ResourceTypeConfig;

public abstract class AbstarctConfigOptionConverter<S extends ConfigOption> implements
            Converter<S, ResourceTypeConfig> {

    public static enum ResourceIdentifierType {
        INT("integer"), IP("ip"), STRING("string"), HOST("host");

        private final String value;

        ResourceIdentifierType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public ResourceTypeConfig baseConvert(ConfigOption configOption) {
        String name = configOption.getName();
        String description = configOption.getDescription();
        String type = ResourceIdentifierType.STRING.toString();
        boolean mandatory = !configOption.isOptional();
        String defaultValue = configOption.getDefault();
        String displayName = configOption.getName();
        boolean readonly = configOption.isReadonly();
        Boolean advanced = configOption.isAdvanced();

        ResourceTypeConfig resourceTypeConfig =
                    new ResourceTypeConfig(name, displayName, description, type, mandatory, defaultValue, readonly,
                                advanced);

        return resourceTypeConfig;
    }

    @Override
    public abstract ResourceTypeConfig convert(S source);
}
