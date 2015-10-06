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

import static org.junit.Assert.assertEquals;

import org.hyperic.hq.product.MeasurementInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.epops.plugin.converter.MeasurementInfoConverter;
import com.vmware.epops.plugin.model.ResourceTypeMeasurement;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class MeasurementInfoConverterTest {

    private String mesName;
    private String mesAlias;
    private String mesCategory;
    private String mesUnits;

    @Autowired
    private MeasurementInfoConverter mesInfoConverter;

    @Before
    public void setup() {
        mesAlias = "MeasurementAlias";
        mesName = "Measurement Name";
        mesCategory = "Throughput";
        mesUnits = "bytes";
    }

    @Test
    public void measurementInfoTestMonitoredOn() {
        ResourceTypeMeasurement rtMes = createMeasurementInfoAndConvert();
        rtMes.setDefaultMonitored(true);
        verifyResourceTypeMeasurement(rtMes, true, false, false);
    }

    @Test
    public void measurementInfoTestMonitoredOff() {
        ResourceTypeMeasurement rtMes = createMeasurementInfoAndConvert();
        rtMes.setDefaultMonitored(false);
        verifyResourceTypeMeasurement(rtMes, false, false, false);
    }

    @Test
    public void measurementInfoTestIndicatorOn() {
        ResourceTypeMeasurement rtMes = createMeasurementInfoAndConvert();
        rtMes.setIndicator(true);
        verifyResourceTypeMeasurement(rtMes, false, true, false);
    }

    @Test
    public void measurementInfoTestIndicatorOff() {
        ResourceTypeMeasurement rtMes = createMeasurementInfoAndConvert();
        rtMes.setIndicator(false);
        verifyResourceTypeMeasurement(rtMes, false, false, false);
    }

    @Test
    public void measurementInfoTestDiscreteOn() {
        ResourceTypeMeasurement rtMes = createMeasurementInfoAndConvert();
        rtMes.setDiscrete(true);
        verifyResourceTypeMeasurement(rtMes, false, false, true);
    }

    @Test
    public void measurementInfoTestDiscreteOff() {
        ResourceTypeMeasurement rtMes = createMeasurementInfoAndConvert();
        rtMes.setDiscrete(false);
        verifyResourceTypeMeasurement(rtMes, false, false, false);
    }

    private ResourceTypeMeasurement createMeasurementInfoAndConvert() {
        MeasurementInfo mi = new MeasurementInfo();
        mi.setName(mesName);
        mi.setAlias(mesAlias);
        mi.setCategory(mesCategory);
        mi.setUnits("B");
        // Convert to ResourceTypeMeasurement
        ResourceTypeMeasurement rtMes = mesInfoConverter.convert(mi);
        return rtMes;
    }

    private void verifyResourceTypeMeasurement(ResourceTypeMeasurement rtMes,
                                               boolean defaultMonitored,
                                               boolean indicator,
                                               boolean discrete) {

        assertEquals("Measurement Names should be identical", mesName, rtMes.getName());
        assertEquals("Measurement Key should be identical to measurment name with space free",
                    mesName.replaceAll(" ", ""), rtMes.getKey());
        assertEquals("Measurement Categories should be identical", mesCategory, rtMes.getGroup());
        assertEquals("Measurement Default Monitored flags should be identical", defaultMonitored,
                    rtMes.isDefaultMonitored());
        assertEquals("Measurement Indicators should be identical", indicator, rtMes.isIndicator());
        assertEquals("Measurement Units should be identical", mesUnits, rtMes.getUnits());
        assertEquals("Measurement Discrete flags should be identical", discrete, rtMes.isDiscrete());

    }
}
