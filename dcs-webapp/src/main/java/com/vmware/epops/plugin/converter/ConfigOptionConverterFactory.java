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

import java.util.Map;

import javax.annotation.Resource;

import org.hyperic.util.config.ConfigOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import com.vmware.epops.plugin.model.ResourceTypeConfig;

public class ConfigOptionConverterFactory implements TypeConverterFactory<ConfigOption, ResourceTypeConfig> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigOptionConverterFactory.class.getName());
    @Resource(name = "configConverters")
    private Map<Class<? extends ConfigOption>, Converter<ConfigOption, ResourceTypeConfig>> configConverters;

    @Override
    public Converter<ConfigOption, ResourceTypeConfig> getConverter(Class<? extends ConfigOption> sourceType) {
        Converter<ConfigOption, ResourceTypeConfig> converter = configConverters.get(sourceType);
        if (converter != null) {
            return configConverters.get(sourceType);
        }
        LOGGER.debug("Conveter not found for class {} - return Default converter for ConfigOption.class", sourceType);
        return configConverters.get(ConfigOption.class);
    }
}
