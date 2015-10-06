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

package org.hyperic.util.vmware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;

public class VMwareGuestInfo {

    private static final String PTQL_QUERY = "State.Name.eq=vmtoolsd";
    private static final String VMTOOLSD_CMD_ARG = "--cmd";
    private static final String VMTOOLSD_GET_CMD_VALUE_PREFIX = "info-get guestinfo.";
    private static final String VMTOOLSD_SET_CMD_VALUE_PREFIX = "info-set guestinfo.";

    // get vmtoolsd
    public static String findVMwareToolsCommand(Sigar sigar) {
        long[] pids;
        try {
            pids = ProcessFinder.find(sigar, PTQL_QUERY);
        } catch (SigarException e) {
            return null;
        }

        for (int i = 0; i < pids.length; i++) {
            try {
                return sigar.getProcExe(pids[i]).getName();
            } catch (SigarException e) {
                // fallthru
            }

            try {
                String[] args = sigar.getProcArgs(pids[i]);
                if (args.length > 0) {
                    return args[0];
                }
            } catch (SigarException e) {
            }
        }

        return null;
    }

    /**
     * get values in machine's VMware GuestInfo of the passed keys.
     * 
     * @param keys
     * @param sigar
     * @return
     */
    public static Map<String, String> getGuestInfoValuesFor(String[] keys,
                                                            Sigar sigar) {
        Map<String, String> info = new HashMap<String, String>();

        String vmtoolsd = findVMwareToolsCommand(sigar);

        if (vmtoolsd == null) {
            return null;
        }

        String[] argv = { vmtoolsd, VMTOOLSD_CMD_ARG, null };

        for (int i = 0; i < keys.length; i++) {
            argv[2] = VMTOOLSD_GET_CMD_VALUE_PREFIX + keys[i];
            String value = getGuestInfoValue(argv);
            if (value != null) {
                info.put(keys[i], value);
            }
        }
        if (info.size() > 0) {
            return info;
        } else {
            return null;
        }
    }

    private static String getGuestInfoValue(String[] argv) {
        Process proc;
        BufferedReader in = null;
        String line;

        try {
            proc = Runtime.getRuntime().exec(argv);
            in = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));
            while ((line = in.readLine()) != null) {
                // Seen on windows:
                // Warning: GuestApp: no value for option 'log'
                if (line.startsWith("Warning:") ||
                            line.startsWith("No value found"))
                {
                    continue;
                }
                return line;
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static void setGuestInfoValue(String key,
                                         String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return;
        }
        Sigar sigar = new Sigar();
        try {
            setGuestInfoValue(sigar, key, value);
        } finally {
            sigar.close();
        }
    }

    public static void setGuestInfoValue(Sigar sigar,
                                         String key,
                                         String value) {

        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return;
        }

        String vmtoolsd = findVMwareToolsCommand(sigar);
        if (vmtoolsd == null) {
            return;
        }
        String[] argv = { vmtoolsd, VMTOOLSD_CMD_ARG, VMTOOLSD_SET_CMD_VALUE_PREFIX + key + " " + value };
        try {
            Runtime.getRuntime().exec(argv);
        } catch (IOException e) {
        }
    }
}
