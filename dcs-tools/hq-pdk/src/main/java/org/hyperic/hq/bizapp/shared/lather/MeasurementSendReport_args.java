/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.MeasurementReportConstructor;
import org.hyperic.hq.measurement.data.ValueList;
import org.hyperic.hq.product.MetricValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MeasurementSendReport_args extends LatherValue {
    private static final String logCtx =
                MeasurementSendReport_args.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);

    private static final String PROP_CIDLIST = "cids";
    private static final String PROP_DSNIDLIST = "dsnidss";
    private static final String PROP_TSTAMPLIST = "tstamps";
    private static final String PROP_VALUELIST = "values";
    private static final String PROP_SRN_ENT_TYPE = "srnEntType";
    private static final String PROP_SRN_ENT_ID = "srnEntId";
    private static final String PROP_SRN_REVNO = "srnRevNo";

    public MeasurementSendReport_args() {
        super();
    }

    public void setReport(MeasurementReport report) {
        DSNList[] clientIDs;
        SRN[] srnList;

        clientIDs = report.getClientIdList();
        for (int cidIdx = 0; cidIdx < clientIDs.length; cidIdx++) {
            ValueList[] dsns = clientIDs[cidIdx].getDsns();

            for (int dsnIdx = 0; dsnIdx < dsns.length; dsnIdx++) {
                MetricValue[] vals = dsns[dsnIdx].getValues();

                for (int valIdx = 0; valIdx < vals.length; valIdx++) {
                    this.addLongToList(PROP_CIDLIST,
                                clientIDs[cidIdx].getClientId());
                    this.addLongToList(PROP_DSNIDLIST,
                                dsns[dsnIdx].getDsnId());
                    this.addDoubleToList(PROP_TSTAMPLIST,
                                vals[valIdx].getTimestamp());
                    this.addDoubleToList(PROP_VALUELIST,
                                vals[valIdx].getValue());
                }
            }
        }

        srnList = report.getSRNList();
        for (int i = 0; i < srnList.length; i++) {
            AppdefEntityID ent = srnList[i].getEntity();

            this.addIntToList(PROP_SRN_ENT_TYPE, ent.getType());
            this.addIntToList(PROP_SRN_ENT_ID, ent.getID());
            this.addIntToList(PROP_SRN_REVNO, srnList[i].getRevisionNumber());
        }
    }

    public MeasurementReport getReport()
        throws LatherRemoteException {
        MeasurementReportConstructor con;
        double[] tStampList;
        double[] valueList;
        long[] cidList, dsnIdList;
        int[] srnEntTypeList, srnEntIdList, srnRevNoList;
        SRN[] srnList;

        if (isEmptyReport()) {
            return createMeasurementReport(new DSNList[] {}, new SRN[] {});
        }

        con = new MeasurementReportConstructor();

        cidList = this.getLongList(PROP_CIDLIST);
        dsnIdList = this.getLongList(PROP_DSNIDLIST);
        tStampList = this.getDoubleList(PROP_TSTAMPLIST);
        valueList = this.getDoubleList(PROP_VALUELIST);

        srnEntTypeList = this.getIntList(PROP_SRN_ENT_TYPE);
        srnEntIdList = this.getIntList(PROP_SRN_ENT_ID);
        srnRevNoList = this.getIntList(PROP_SRN_REVNO);

        if (dsnIdList.length != tStampList.length ||
                    dsnIdList.length != valueList.length ||
                    dsnIdList.length != cidList.length ||
                    srnEntTypeList.length != srnEntIdList.length ||
                    srnEntTypeList.length != srnRevNoList.length)
        {
            throw new LatherRemoteException("Measurement report mismatch");
        }

        for (int i = 0; i < dsnIdList.length; i++)
        {
            con.addDataPoint(cidList[i], dsnIdList[i],
                        new MetricValue(valueList[i],
                                    (long) tStampList[i]));
        }

        srnList = new SRN[srnEntTypeList.length];

        for (int i = 0; i < srnEntTypeList.length; i++) {
            AppdefEntityID ent = new AppdefEntityID(srnEntTypeList[i],
                        srnEntIdList[i]);
            SRN srn = new SRN(ent, srnRevNoList[i]);

            srnList[i] = srn;
        }

        return createMeasurementReport(con.constructDSNList(), srnList);
    }

    private MeasurementReport createMeasurementReport(DSNList[] dsnLists,
                                                      SRN[] srns) {
        MeasurementReport measurmentReport = new MeasurementReport();
        measurmentReport.setClientIdList(dsnLists);
        measurmentReport.setSRNList(srns);
        return measurmentReport;
    }

    /**
     * Empty report contains no client ids and no dsn entries
     */
    private boolean isEmptyReport() {
        return !(this.getLongLists().containsKey(PROP_CIDLIST) && this.getLongLists().containsKey(PROP_DSNIDLIST));
    }
}
