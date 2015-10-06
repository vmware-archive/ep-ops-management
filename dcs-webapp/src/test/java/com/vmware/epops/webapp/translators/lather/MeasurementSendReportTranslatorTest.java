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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vmware.epops.command.upstream.measurement.MeasurementReportCommandData;
import com.vmware.epops.command.upstream.measurement.MetricVal;
import com.vmware.epops.model.RawResource;
import com.vmware.epops.webapp.translators.lather.MeasurementSendReportTranslator;

import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_args;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.ValueList;
import org.hyperic.hq.product.MetricValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class MeasurementSendReportTranslatorTest extends TranslatorUtil {
    private MeasurementSendReportTranslator tested;
    private MeasurementSendReport_args mockedArgs;
    private DSNList[] clientIDs;
    private static int SIZE = 12;
    Map<Integer, Multimap<Integer, MetricValue>> metricsMap = new HashMap<>();

    @Before
    public void setUp()
        throws Exception {
        tested = new MeasurementSendReportTranslator();

        mockedArgs = Mockito.mock(MeasurementSendReport_args.class, Mockito.RETURNS_DEEP_STUBS);
        long time = 1;
        long dsnId = 1073741824l; // this number will cause the translator to aggregate metric keys and values
        // to the same RawResource (the first one)
        clientIDs = new DSNList[SIZE];
        for (int i = 0; i < SIZE; i++) {
            DSNList client = new DSNList();

            ValueList[] valueList = new ValueList[SIZE];
            for (int x = 0; x < SIZE; x++) {
                ValueList value = new ValueList();
                value.setDsnId(dsnId);
                MetricValue[] metrics = new MetricValue[SIZE];
                for (int j = 0; j < SIZE; j++) {
                    metrics[j] = new MetricValue(j + i, time++);
                    Multimap<Integer, MetricValue> integerMetricValueMultimap =
                                metricsMap.get((int) (value.getDsnId() >> 32));
                    if (integerMetricValueMultimap == null) {
                        integerMetricValueMultimap = ArrayListMultimap.create();
                        metricsMap.put((int) (value.getDsnId() >> 32), integerMetricValueMultimap);
                    }
                    integerMetricValueMultimap.put((int) dsnId, metrics[j]);
                }
                value.setValues(metrics);
                valueList[x] = value;
            }
            dsnId *= 2;
            client.setDsns(valueList);
            clientIDs[i] = client;
        }

        Mockito.when(mockedArgs.getReport().getClientIdList()).thenReturn(clientIDs);
    }

    @Test
    public void testTranslate()
        throws Exception {
        tested = new MeasurementSendReportTranslator();
        MeasurementReportCommandData measurementReportCommandData =
                    (MeasurementReportCommandData) tested.translateRequest(mockedArgs, AGENT_TOKEN);

        List<RawResource> rawResources = measurementReportCommandData.getRawResources();
        Assert.assertEquals(metricsMap.keySet().size(), rawResources.size());
        for (RawResource rawResource : rawResources) {
            Multimap<Integer, MetricValue> integerMetricValueMultimap = metricsMap.get(rawResource.getInternalId());
            Map<Integer, List<MetricVal>> metrics = rawResource.getMetrics();
            Assert.assertEquals(integerMetricValueMultimap.keySet().size(), metrics.size());

            for (Integer id : integerMetricValueMultimap.keySet()) {
                List<MetricVal> resultMetricVals = metrics.get(id);
                Assert.assertNotNull(resultMetricVals);
                Collection<MetricValue> expectedMetricValues = integerMetricValueMultimap.get(id);
                int i = 0;
                for (MetricValue expectedMetricValue : expectedMetricValues) {
                    MetricVal metricVal = resultMetricVals.get(i++);
                    Assert.assertTrue(new Double(expectedMetricValue.getValue()).compareTo(metricVal.getMetricValue()) <= 0.000001);
                    Assert.assertEquals(expectedMetricValue.getTimestamp(), metricVal.getTimestamp());
                }
            }
        }
    }
}
