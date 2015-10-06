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

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.MeasurementInfo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.vmware.epops.plugin.model.ResourceTypeMeasurement;

/**
 * Converts MeasurementInfo into ResourceTypeMeasurement
 */
@Component
public class MeasurementInfoConverter implements Converter<MeasurementInfo, ResourceTypeMeasurement> {

    private Map<String, String> unitsConversionMap;

    public MeasurementInfoConverter() {
        initializeUnitsMap();
    }

    @Override
    public ResourceTypeMeasurement convert(MeasurementInfo measInfo) {
        ResourceTypeMeasurement resourceTypeMeasurement = new ResourceTypeMeasurement();
        resourceTypeMeasurement.setName(NameMappingUtil.convertMetricName(measInfo.getName()));
        resourceTypeMeasurement.setKey(convertMetricNameKey(measInfo.getName()));
        resourceTypeMeasurement.setDefaultMonitored(measInfo.isDefaultOn());
        resourceTypeMeasurement.setGroup(measInfo.getCategory());
        resourceTypeMeasurement.setIndicator(measInfo.isIndicator());
        resourceTypeMeasurement.setUnits("none");
        if (!MeasurementConstants.CAT_AVAILABILITY.equals(measInfo.getCategory())) {
            resourceTypeMeasurement.setUnits(unitsConversionMap.get(measInfo.getUnits()));
        }
        // The value 1 means "static" which is the same as "discrete"
        if (measInfo.getCollectionType() == 1) {
            resourceTypeMeasurement.setDiscrete(true);
        }
        return resourceTypeMeasurement;
    }

    private String convertMetricNameKey(String metricNameKey) {
        return NameMappingUtil.convertMetricName(metricNameKey).replaceAll(" ", "");

    }

    private void initializeUnitsMap() {
        unitsConversionMap = new HashMap<>();

        // Simple metric types
        unitsConversionMap.put(MeasurementConstants.UNITS_NONE, "none");
        unitsConversionMap.put(MeasurementConstants.UNITS_PERCENTAGE, "percent");
        unitsConversionMap.put(MeasurementConstants.UNITS_PERCENT, "percent");

        // Bytes
        unitsConversionMap.put(MeasurementConstants.UNITS_BYTES, "bytes");
        unitsConversionMap.put(MeasurementConstants.UNITS_KBYTES, "kb");
        unitsConversionMap.put(MeasurementConstants.UNITS_MBYTES, "mb");
        unitsConversionMap.put(MeasurementConstants.UNITS_GBYTES, "gb");
        unitsConversionMap.put(MeasurementConstants.UNITS_TBYTES, "tb");
        unitsConversionMap.put(MeasurementConstants.UNITS_PBYTES, "pb");

        // convert bytesToBits
        unitsConversionMap.put(MeasurementConstants.UNITS_BYTES_TO_BITS, "bit");

        // Bits
        unitsConversionMap.put(MeasurementConstants.UNITS_BITS, "bit");
        unitsConversionMap.put(MeasurementConstants.UNITS_KBITS, "kibit");
        unitsConversionMap.put(MeasurementConstants.UNITS_MBITS, "mibit");
        unitsConversionMap.put(MeasurementConstants.UNITS_GBITS, "gibit");
        unitsConversionMap.put(MeasurementConstants.UNITS_TBITS, "tibit");
        unitsConversionMap.put(MeasurementConstants.UNITS_PBITS, "pibit");

        // Time since January 1, 1970
        unitsConversionMap.put(MeasurementConstants.UNITS_EPOCH_MILLIS, "epoch-millis");
        unitsConversionMap.put(MeasurementConstants.UNITS_EPOCH_SECONDS, "epoch-seconds");

        // Elapsed time (response time)
        unitsConversionMap.put(MeasurementConstants.UNITS_NANOS, "ns");
        unitsConversionMap.put(MeasurementConstants.UNITS_MICROS, "mu");
        unitsConversionMap.put(MeasurementConstants.UNITS_MILLIS, "ms");
        unitsConversionMap.put(MeasurementConstants.UNITS_SECONDS, "sec");

        // Unused units
        unitsConversionMap.put(MeasurementConstants.UNITS_CENTS, "none");
        unitsConversionMap.put(MeasurementConstants.UNITS_JIFFYS, "none");
    }
}
