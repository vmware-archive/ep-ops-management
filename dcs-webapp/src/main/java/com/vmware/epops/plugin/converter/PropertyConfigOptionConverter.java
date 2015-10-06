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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import com.vmware.epops.plugin.model.ResourceTypeConfig;

public class PropertyConfigOptionConverter extends AbstarctConfigOptionConverter<PropertyConfigOption> {

    @Autowired
    private ResourceConverter resourceConverter;

    @Override
    public ResourceTypeConfig convert(PropertyConfigOption source) {
        ConfigOption configOption = source.getConfigOption();

        ConfigOptionConverterFactory configOptionConverter = resourceConverter.getConfigOptionConverter();
        // Convert ConfigOption to ResourceTypeConfig
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverter.getConverter(configOption.getClass());
        ResourceTypeConfig resourceTypeConfig = converter.convert(configOption);

        resourceTypeConfig.setDisplayName(configOption.getDescription());
        return resourceTypeConfig;
    }

}
