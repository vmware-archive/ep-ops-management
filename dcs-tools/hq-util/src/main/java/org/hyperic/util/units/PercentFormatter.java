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

package org.hyperic.util.units;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class PercentFormatter extends PercentageFormatter {

    // example use-case from CISCO-PROCESS-MIB:
    // cpmCPUTotal5secRev OBJECT-TYPE
    // SYNTAX Gauge32 (0..100)
    // UNITS "percent"

    protected double getMultiplier() {
        return 1.0;
    }

    protected int getUnitType() {
        return UnitsConstants.UNIT_PERCENT;
    }

    /* (non-Javadoc)
     * @see org.hyperic.util.units.PercentageFormatter#parse(java.lang.String, java.util.Locale, org.hyperic.util.units.ParseSpecifics)
     */
    public UnitNumber parse(String val,
                            Locale locale,
                            ParseSpecifics specifics)
        throws ParseException {
        NumberFormat fmt;

        fmt = NumberFormat.getNumberInstance(locale);
        return new UnitNumber(fmt.parse(val).doubleValue(),
                    this.getUnitType(), this.getUnitScale());
    }
}
