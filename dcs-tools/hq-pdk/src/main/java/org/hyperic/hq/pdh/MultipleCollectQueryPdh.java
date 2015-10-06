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

package org.hyperic.hq.pdh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

/**
 * This class is responsible for PDH queries. PDH (Performance Data Helper) APIs can be used to collect performance data
 * of performance counters that are available on the system. As you may notice, there's a similar Pdh class at Sigar.
 * But, this PDH here comes to handle counters that should be queried twice with an (at least) 1sec interval. As shown
 * at: http://msdn.microsoft.com/en-us/library/windows/desktop/aa371897(v=vs.85).aspx This is the reason we use bulk
 * queries instead of single ones (one sleep fits all).
 */
public class MultipleCollectQueryPdh {
    private static final String DLLS_PATH_IN_JAR = "priv_lib/";
    private static final String PDH_DLL_NAME = "hyperic_pdh.dll";
    private static final Log log = LogFactory.getLog(MultipleCollectQueryPdh.class);

    private static native long pdhOpenQuery()
        throws PluginException;

    private static native long pdhAddCounter(long query,
                                             String path)
        throws PluginException;

    private static native void pdhRemoveCounter(long counter)
        throws PluginException;

    private static native void PdhCollectQueryData(long query)
        throws PluginException;

    private static native double PdhGetFormattedCounterValue(long counter)
        throws PluginException;

    private static native String[] pdhGetInstances(String path)
        throws PluginException;

    private static final class InstanceIndex {
        long index = 0;
    }

    /**
     * Get PDH instances (indexing them in order to avoid duplicates)
     */
    public static String[] getInstances(String path)
        throws PluginException {
        String[] instances = pdhGetInstances(path);

        HashMap<String, InstanceIndex> names = new HashMap<String, InstanceIndex>(instances.length);
        for (int i = 0; i < instances.length; i++) {
            InstanceIndex ix = names.get(instances[i]);
            if (ix == null) {
                ix = new InstanceIndex();
                names.put(instances[i], ix);
            } else {
                ix.index++;
                instances[i] = instances[i] + "#" + ix.index;
            }
        }

        return instances;
    }

    public static Map<String, Double> getFormattedValues(List<String> paths)
        throws PluginException, InterruptedException, Win32Exception {
        Map<String, Double> res = new HashMap<String, Double>();
        long q = MultipleCollectQueryPdh.pdhOpenQuery();

        Map<String, Long> counters = addCounters(q, paths, res);
        if (!counters.isEmpty()) {
            try {
                collectDataTwice(q);
                getFormattedCounterValues(paths, counters, res);
            } finally {
                removeCounters(counters);
            }
        }

        return res;
    }

    public static double getValue(String path)
        throws PluginException, Win32Exception {
        double res = MetricValue.NONE.getValue();
        long q = MultipleCollectQueryPdh.pdhOpenQuery();
        long counter = addCounter(path, q);
        try {
            PdhCollectQueryData(q);
            res = PdhGetFormattedCounterValue(counter);
        } finally {
            pdhRemoveCounter(counter);
        }

        return res;
    }

    /**
     * Some counters should be queried twice with an (at least) 1sec interval. Therefore here is a method for collecting
     * such counters.
     */
    private static void collectDataTwice(long q)
        throws PluginException, InterruptedException {
        PdhCollectQueryData(q);
        Thread.sleep(1000);
        PdhCollectQueryData(q);
    }

    private static void removeCounters(Map<String, Long> counters) {
        for (Long counter : counters.values()) {
            try {
                pdhRemoveCounter(counter);
            } catch (PluginException ex) {
                log.debug("Error removing counters", ex);
            }
        }
    }

    private static void getFormattedCounterValues(List<String> paths,
                                                  Map<String, Long> counters,
                                                  Map<String, Double> res) {
        for (String path : paths) {
            try {
                Long c = counters.get(path);
                if (c != null) {
                    Double val = PdhGetFormattedCounterValue(c);
                    log.debug("formatted path: '" + path + "' value:" + val);
                    res.put(path, val);
                }
            } catch (PluginException ex) {
                log.debug("Error getting metric value of path: " + path, ex);
            }
        }
    }

    private static Map<String, Long> addCounters(long q,
                                                 List<String> paths,
                                                 Map<String, Double> res)
        throws Win32Exception {
        Map<String, Long> counters = new HashMap<String, Long>();
        for (String path : paths) {
            try {
                counters.put(path, addCounter(path, q));
            } catch (PluginException ex) {
                log.debug("Error adding metric of path: " + path, ex);
                res.put(path, Double.NaN);
            }
        }

        return counters;
    }

    /**
     * Adding counters.
     * 
     * In order to support localization additional steps are being done: (1) Counters should be translated to localized
     * counterparts in order to support localized windows systems. (2) Some metrics are missing the '/sec' suffix which
     * would make translation fail.
     */
    private static long addCounter(String path,
                                   long q)
        throws PluginException, Win32Exception {
        long counter;
        try {
            counter = pdhAddCounter(q, Pdh.translate(path));
        } catch (Win32Exception e) {
            log.debug("couldn't add counter. trying again with '/sec' suffix...", e);
            counter = pdhAddCounter(q, Pdh.translate(path + "/sec"));
        }

        return counter;
    }

    static {
        String libPathInJar = getLibPathInJar();
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            URL in = MultipleCollectQueryPdh.class.getClassLoader().getResource(libPathInJar);
            if (in == null) {
                throw new FileNotFoundException(libPathInJar);
            }

            File out = new File(System.getProperty("java.io.tmpdir"), PDH_DLL_NAME);
            log.info("[static] Writing dll to: " + out.getAbsolutePath());
            inStream = in.openStream();
            outStream = new FileOutputStream(out);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

            closeStreams(inStream, outStream);
            System.load(out.getAbsolutePath());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        } finally {
            closeStreams(inStream, outStream);
        }
    }

    private static void closeStreams(InputStream inStream,
                                     OutputStream outStream) {
        if (outStream != null) {
            try {
                outStream.close();
            } catch (IOException ignore) { /*ignore*/
            }
        }
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException ignore) { /*ignore*/
            }
        }
    }

    private static String getLibPathInJar() {
        StringBuilder libPathStrBuilder = new StringBuilder();
        libPathStrBuilder.append(DLLS_PATH_IN_JAR);
        String os = System.getProperty("os.arch");
        libPathStrBuilder.append(os.contains("64") ? "x64/" : "win32/");
        libPathStrBuilder.append(PDH_DLL_NAME);

        return libPathStrBuilder.toString();
    }
}
