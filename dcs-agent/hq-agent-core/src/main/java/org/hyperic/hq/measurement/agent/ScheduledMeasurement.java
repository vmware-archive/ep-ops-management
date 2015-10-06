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

package org.hyperic.hq.measurement.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.encoding.Base64;

public class ScheduledMeasurement {
    private String dsn;
    private long interval;
    private Long derivedID;
    private Long dsnID;
    private AppdefEntityID ent;
    private String category;
    private long lastCollected;
    private String units;

    public ScheduledMeasurement(String dsn,
                                long interval,
                                long derivedId,
                                long dsnId,
                                AppdefEntityID ent,
                                String category,
                                String units)
    {
        this.dsn = dsn;
        this.interval = interval;
        this.derivedID = derivedId;
        this.dsnID = dsnId;
        this.ent = ent;
        this.category = category;
        this.units = units;
    }

    public String toString()
    {
        long derviedId = getDerivedID(), dsnId = getDsnID();
        String dsn = getDSN();
        StringBuffer buf = new StringBuffer(64);
        buf.append("[derivedId=").append(getDerivedID()).
                    append("|dsnId=").append(getDsnID()).append("]").
                    append("|interval=").append(getInterval()).append("]");
        return buf.toString();
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

    public long getDsnID() {
        return this.dsnID;
    }

    public AppdefEntityID getEntity() {
        return this.ent;
    }

    public String getCategory() {
        return this.category;
    }

    public String getUnits() {
        return this.units;
    }

    public String encode() {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;

        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);

        try {
            dOs.writeUTF(this.dsn);
            dOs.writeLong(this.interval);
            dOs.writeLong(this.derivedID);
            dOs.writeLong(this.dsnID);
            dOs.writeInt(this.ent.getType());
            dOs.writeInt(this.ent.getID());
            dOs.writeUTF(this.category);
            dOs.writeUTF(this.units);
        } catch (IOException exc) {
            // Shouldn't ever occur, but ...
            System.out.println("Unable to encode record: " + exc.getMessage());
        }

        return Base64.encode(bOs.toByteArray());
    }

    static public ScheduledMeasurement decode(String data) {
        ByteArrayInputStream bIs;
        DataInputStream dIs;

        bIs = new ByteArrayInputStream(Base64.decode(data));
        dIs = new DataInputStream(bIs);

        try {
            String dsn = dIs.readUTF();
            long interval = dIs.readLong();
            long derivedID = dIs.readLong();
            long dsnID = dIs.readLong();
            int entType = dIs.readInt();
            int entID = dIs.readInt();
            String category = dIs.readUTF();
            String units = dIs.readUTF();

            return new ScheduledMeasurement(dsn, interval, derivedID, dsnID,
                        new AppdefEntityID(entType, entID),
                        category, units);
        } catch (IOException exc) {
            // Shouldn't ever occur, but ...
            System.out.println("Unable to encode record: " + exc.getMessage());
            return null;
        }
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs instanceof ScheduledMeasurement) {
            ScheduledMeasurement m = (ScheduledMeasurement) rhs;
            return derivedID == m.derivedID;
        }
        return false;
    }

    public int hashCode() {
        return derivedID.hashCode();
    }

    public long getLastCollected() {
        return lastCollected;
    }

    public void setLastCollected(long lastCollected) {
        this.lastCollected = lastCollected;
    }

}
