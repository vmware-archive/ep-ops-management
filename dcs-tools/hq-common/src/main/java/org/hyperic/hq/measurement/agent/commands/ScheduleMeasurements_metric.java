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

package org.hyperic.hq.measurement.agent.commands;

public class ScheduleMeasurements_metric {
    private String dsn;
    private long interval;
    private long derivedID;
    private long dsnID;
    private String category;
    private String units;

    ScheduleMeasurements_metric(String dsn,
                                long interval,
                                long derivedID,
                                long dsnID,
                                String category,
                                String units)
    {
        this.dsn = dsn;
        this.interval = interval;
        this.derivedID = derivedID;
        this.dsnID = dsnID;
        this.category = category;
        this.units = units;
    }

    public String getDSN() {
        return this.dsn;
    }

    public long getInterval() {
        return this.interval;
    }

    public long getDerivedID() {
        return this.derivedID;
    }

    public long getDSNID() {
        return this.dsnID;
    }

    public String getCategory() {
        return this.category;
    }

    public String getUnits() {
        return this.units;
    }
}
