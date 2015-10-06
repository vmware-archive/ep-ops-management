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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.util.config.ConfigOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.vmware.epops.plugin.model.ResourceTypeConfig;
import com.vmware.epops.plugin.model.ResourceTypeMeasurement;

@Component
public class PluginConverterUtil {

    @Autowired
    private ResourceConverter resourceConverter;

    public List<ResourceTypeConfig> convertConfigurations(Collection<? extends ConfigOption> unifiedConfigurations) {
        List<ResourceTypeConfig> resourceConfigs = new ArrayList<ResourceTypeConfig>();
        ConfigOptionConverterFactory configOptionConverter = resourceConverter.getConfigOptionConverter();
        for (ConfigOption configOption : unifiedConfigurations) {
            // Convert ConfigOption to ResourceTypeConfig
            Converter<ConfigOption, ResourceTypeConfig> converter =
                        configOptionConverter.getConverter(configOption.getClass());
            ResourceTypeConfig resourceTypeConfig = converter.convert(configOption);
            resourceConfigs.add(resourceTypeConfig);
        }
        return resourceConfigs;
    }

    public List<ResourceTypeMeasurement> convertMeasurements(List<MeasurementInfo> measurements) {
        List<ResourceTypeMeasurement> rsourceTypeMeasurements = new ArrayList<ResourceTypeMeasurement>();
        MeasurementInfoConverter measurementInfoConverter = resourceConverter.getMeasurementInfoConverter();
        for (MeasurementInfo measurementInfo : measurements) {
            ResourceTypeMeasurement resourceTypeMeasurement = measurementInfoConverter.convert(measurementInfo);
            rsourceTypeMeasurements.add(resourceTypeMeasurement);
        }
        return rsourceTypeMeasurements;
    }

}
