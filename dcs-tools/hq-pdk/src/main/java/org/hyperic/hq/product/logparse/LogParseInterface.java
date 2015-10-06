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

package org.hyperic.hq.product.logparse;

import java.util.ArrayList;
import java.util.Hashtable;
import java.io.File;
import java.io.IOException;

import org.hyperic.hq.product.RtStat;

public interface LogParseInterface {

    public void initConfig(double timeMultiplier,
                           String regex);

    public RtStat parseLine(String currrent);

    public Hashtable parseLog(String fname,
                              String re,
                              Integer ID,
                              boolean collectIPs)
        throws IOException;

    public Hashtable parseLog(String fname,
                              String re,
                              Integer ID,
                              int svcType,
                              boolean collectIPs)
        throws IOException;

    public Hashtable parseLog(String fname,
                              String re,
                              long len,
                              Integer ID,
                              boolean collectIPs)
        throws IOException;

    public Hashtable parseLog(String fname,
                              String re,
                              long len,
                              Integer ID,
                              int svcType,
                              long parsedlen[],
                              boolean collectIPs)
        throws IOException;

    public Hashtable parseLog(File f,
                              String re,
                              long len,
                              Integer ID,
                              int svcType,
                              long parsedlen[],
                              boolean collectIPs)
        throws IOException;

    public void setTimeMultiplier(double mult);

    public double getTimeMultiplier();

    public void DontLog(Long stat);

    public void DontLog(String url);

    public void urlDontLog(ArrayList urls);

    public void postFileParse(File f)
        throws IOException;
}
