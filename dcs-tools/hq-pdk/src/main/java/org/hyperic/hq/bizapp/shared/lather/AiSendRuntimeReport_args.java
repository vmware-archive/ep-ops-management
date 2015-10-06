/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

public class AiSendRuntimeReport_args extends LatherValue {

    private static final String PROP_REPORT = "report";

    public AiSendRuntimeReport_args() {
        super();
    }

    public AiSendRuntimeReport_args(CompositeRuntimeResourceReport report) {
        super();
        this.setObjectValue(PROP_REPORT,
                    new CompositeRuntimeResourceReportLatherValue(report));

        this.setStringValue(HQConstants.SYNC_SCAN,
                    Boolean.toString(report.isSyncScan()));
    }

    public CompositeRuntimeResourceReport getReport() {
        CompositeRuntimeResourceReportLatherValue res;

        res = (CompositeRuntimeResourceReportLatherValue)
                    this.getObjectValue(PROP_REPORT);
        return res.getReport();
    }

    public void validate()
        throws LatherRemoteException {
        super.validate();
        try {
            this.getReport();
        } catch (LatherKeyNotFoundException exc) {
            throw new LatherRemoteException("All values not set");
        }
    }
}
