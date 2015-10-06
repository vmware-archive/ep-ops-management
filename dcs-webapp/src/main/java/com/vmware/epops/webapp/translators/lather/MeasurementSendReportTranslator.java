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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_result;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.ValueList;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.upstream.EmptyAgentResponse;
import com.vmware.epops.command.upstream.measurement.MeasurementReportCommandData;
import com.vmware.epops.command.upstream.measurement.MetricVal;
import com.vmware.epops.model.RawResource;
import com.vmware.epops.util.CommonUtils;

/**
 * @author rina
 */
public class MeasurementSendReportTranslator implements AgentVerifiedLatherCommandTranslator {
    private final static Logger log =
                LoggerFactory.getLogger(MeasurementSendReportTranslator.class);
    private final static String COMMAND_NAME = CommandInfo.CMD_MEASUREMENT_SEND_REPORT;

    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {
        if (!(latherValue instanceof MeasurementSendReport_args)) {
            log.error("Got {} command request with the wrong values from agent: {}", COMMAND_NAME, agentToken);
            return null;
        }

        MeasurementSendReport_args latherArgs = (MeasurementSendReport_args) latherValue;

        DSNList[] clientIDs;
        List<RawResource> rawData = null;
        try {
            clientIDs = latherArgs.getReport().getClientIdList();
            rawData = getRawData(clientIDs, agentToken);
        } catch (LatherRemoteException e) {
            log.error("Failed to translate MeasurementSendReport_args. Error: {}", e.getMessage());
        }
        MeasurementReportCommandData commandData = new MeasurementReportCommandData(agentToken, rawData);

        return commandData;
    }

    private List<RawResource> getRawData(DSNList[] clientIDs,
                                         String agentToken) {
        Map<Integer, RawResource> resources = new HashMap<>();

        for (int cidIdx = 0; cidIdx < clientIDs.length; cidIdx++) {
            ValueList[] dsns = clientIDs[cidIdx].getDsns();

            for (int dsnIdx = 0; dsnIdx < dsns.length; dsnIdx++) {
                MetricValue[] vals = dsns[dsnIdx].getValues();
                long dsnId = dsns[dsnIdx].getDsnId();
                List<MetricVal> metricValList = getMetricValList(resources, CommonUtils.getResourceId(dsnId),
                            CommonUtils.getAttributeKeyId(dsnId), agentToken);
                for (int valIdx = 0; valIdx < vals.length; valIdx++) {
                    metricValList.add(new MetricVal(vals[valIdx].getValue(), vals[valIdx].getTimestamp()));
                }
            }
        }
        return new ArrayList<RawResource>(resources.values());
    }

    private List<MetricVal> getMetricValList(Map<Integer, RawResource> resources,
                                             Integer resourceId,
                                             Integer metricId,
                                             String agentToken) {
        RawResource rawResource = resources.get(resourceId);
        if (rawResource == null) {
            rawResource = new RawResource(resourceId, new HashMap<Integer, List<MetricVal>>());
            rawResource.setAgentToken(agentToken);
            resources.put(resourceId, rawResource);
        }
        Map<Integer, List<MetricVal>> metrics = rawResource.getMetrics();
        List<MetricVal> metricVals = metrics.get(metricId);
        if (metricVals == null) {
            metricVals = new ArrayList<>();
            metrics.put(metricId, metricVals);
        }
        return metricVals;
    }

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        MeasurementSendReport_result res = new MeasurementSendReport_result();
        res.setTime(System.currentTimeMillis());
        return res;
    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return EmptyAgentResponse.class;
    }

}
