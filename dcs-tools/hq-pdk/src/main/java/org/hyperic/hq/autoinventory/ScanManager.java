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

package org.hyperic.hq.autoinventory;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.util.AutoApproveConfig;
import org.hyperic.util.timer.StopWatch;

/**
 * The ScanManager controls the Scanner and ensures that only 1 scan is running at a time. If someone tries to start a
 * scan while another scan is running, we'll cue up that scan to run immediately after the current scan completes.
 */
public class ScanManager implements ScanListener {

    private final Log log;

    /** Holds the list of queued-up scanners */
    private final LinkedList scannerList = new LinkedList();

    /** Which scanner is currently running? */
    private volatile Scanner activeScanner = null;

    /** Who to notify when scans complete */
    private ScanListener listener = null;

    /** Where we get our plugins from */
    private ProductPluginManager ppm = null;

    /** The thread that runs the scan manager */
    private volatile Thread mainThread = null;

    /** Flag that determines whether the manager should exit or keep running */
    private volatile boolean shouldExit = false;

    /** Is there a scan running right now? */
    private volatile boolean scanInProgress = false;

    /** Trigger Runtime scan in the next iteration. */
    private final SyncModeManager syncModeManager;

    /** Our runtime autodiscovery scanner, can be null */
    private RuntimeScanner rtScanner = null;

    /** The auto-approval configuration instance */
    private final AutoApproveConfig autoApproveConfig;

    /** When was the last time we ran a runtime scan? */
    private long lastRtScan;

    /** When was the last time we ran a regular defaultscan? */
    private long lastDefaultScan;

    /**
     * Create a scan manager.
     * 
     * @param listener The ScanListener to be notified when scans complete.
     * @param log The log to use.
     * @param apm The Autoinventory plugin manager to use for loading platform and server detectors.
     * @param rtScanner The RuntimeScanner to use for runtime scans. This can be null if no runtime scans are to be
     *            performed.
     */
    public ScanManager(ScanListener listener,
                       Log log,
                       ProductPluginManager ppm,
                       RuntimeScanner rtScanner,
                       AutoApproveConfig autoApproveConfig,
                       SyncModeManager syncModeManager) {
        this.listener = listener;
        this.log = log;
        this.ppm = ppm;
        this.rtScanner = rtScanner;
        this.autoApproveConfig = autoApproveConfig;
        this.syncModeManager = syncModeManager;
    }

    /**
     * Startup the ScanManager. This fires off a Thread that manages the scan queue.
     */
    public synchronized void startup() {
        shouldExit = false;
        if (rtScanner != null) {
            lastRtScan = System.currentTimeMillis();
        }
        mainThread = new Thread() {
            @Override
            public void run() {
                try {
                    mainRunLoop();
                } catch (Exception e) {
                    log.error("ERROR in ScanManager thread, exiting: " + e, e);
                }
            }
        };
        mainThread.setPriority(Thread.MIN_PRIORITY);
        mainThread.setDaemon(true);
        mainThread.setName("autoinventory-scanner");
        mainThread.start();
    }

    private void mainRunLoop() {

        while (!shouldExit) {

            synchronized (scannerList) {
                if (scannerList.size() > 0) {
                    activeScanner = (Scanner) scannerList.removeFirst();
                } else {
                    activeScanner = null;
                }
            }

            // If no scanners are in the queue, Schedule a default sync scan,
            // set trigger to rtScan = true
            // update the next scan time and move to next iteration
            if (activeScanner == null && isTimeForSyncScan()) {
                log.info("Starting Sync Scan");
                scheduleDefaultScan(true);
                syncModeManager.setSyncMode(true);
                continue;
            }

            // Even if no scanner was set, we now run the DefaultScan
            // periodically. Find out if we should do that now.
            if (activeScanner == null && isTimeForDefaultScan()) {
                scheduleDefaultScan(false);
                continue;
            }

            if (activeScanner != null) {
                try {
                    scanInProgress = true;
                    activeScanner.start();

                } catch (Exception e) {
                    log.error("Exception starting scanner: " + e, e);

                } catch (NoClassDefFoundError e) {
                    log.error("Error starting scanner: " + e, e);

                } finally {
                    synchronized (scannerList) {
                        activeScanner = null;
                    }
                    scanInProgress = false;
                    // clear the plugin shared data, caches, etc.
                    this.ppm.getAutoinventoryPluginManager().endScan();
                }
            }

            // Check and clear interrupt flag before sleeping
            if (Thread.interrupted())
                continue;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }

            // TODO: shushi - Disable scheduled runtime scans.
            if (isTimeForRtScan()) {
                doRuntimeScan(syncModeManager.isSyncMode());
            }
        }
    }

    private boolean isTimeForSyncScan() {
        if (rtScanner != null) {
            return System.currentTimeMillis() > getNextSyncScanTime();
        }
        return false;
    }

    private long getNextSyncScanTime() {
        return syncModeManager.getNextSyncScanTime();
    }

    private void scheduleDefaultScan(boolean isSyncScan) {
        final StopWatch watch = new StopWatch();
        log.info("starting default scan, Sync Mode:" + isSyncScan);
        if (isSyncScan) {
            rtScanner.scheduleDefaultSyncScan();
        } else {
            rtScanner.scheduleDefaultScan();
        }
        log.info("default scan complete " + watch + ", Sync Mode:" + isSyncScan);
        lastDefaultScan = System.currentTimeMillis();
    }

    private void doRuntimeScan(boolean isSyncScan) {
        try {
            final StopWatch watch = new StopWatch();
            log.info("starting runtime scan, Sync Mode:" + isSyncScan);
            if (isSyncScan) {
                rtScanner.doRuntimeSyncScan();
                syncModeManager.setSyncMode(false);
            } else {
                rtScanner.doRuntimeScan();
            }
            log.info("runtime scan complete " + watch + ", Sync Mode:"
                        + isSyncScan);
        } catch (Exception e) {
            log.error("Error running runtime autodiscovery scan: " + e, e);
        } finally {
            lastRtScan = System.currentTimeMillis();
        }
    }

    private boolean isTimeForRtScan() {
        if (rtScanner == null) {
            return false;
        }
        long interval = rtScanner.getScanInterval();
        return (rtScanner != null && System.currentTimeMillis() - lastRtScan > interval);
    }

    private boolean isTimeForDefaultScan() {
        if (rtScanner == null) {
            return false;
        }
        long interval = rtScanner.getDefaultScanInterval();
        return (System.currentTimeMillis() - lastDefaultScan > interval);
    }

    /**
     * Stop the ScanManager. Any currently running scan will be stopped.
     * 
     * @param timeout How long to wait for the ScanManager to gracefully exit before Thread.stop-ing it.
     */
    public synchronized void shutdown(long timeout) {
        shouldExit = true;
        if (mainThread != null) {
            mainThread.interrupt();
        } else {
            return;
        }
        long startSleep = System.currentTimeMillis();
        while (mainThread.isAlive()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
            if (System.currentTimeMillis() - startSleep > timeout) {
                mainThread.stop();
            }
        }
    }

    /**
     * Start a new scan, using the specified scan configuration.
     * 
     * @param scanConfig The configuration to use for the scan.
     * @return true if a scan with an identical configuration has already been scheduled, false if this is a new
     *         configuration not yet scheduled.
     */
    public boolean queueScan(ScanConfiguration scanConfig) {

        Scanner scanner = new Scanner(scanConfig, this, this.ppm, this.autoApproveConfig);

        synchronized (scannerList) {
            if (scannerList.contains(scanner)) {
                return true; // already queued with this config
            }

            scannerList.addLast(scanner);
            return false;
        }
    }

    public void interruptHangingScan() {
        // If the current scan is sleeping in "scanComplete", interrupt the
        // thread. The server is probably available to receive the report now.
        synchronized (scannerList) {
            if (activeScanner != null) {
                ScanState state = activeScanner.getScanState();
                if (state != null && state.getIsDone()) {
                    mainThread.interrupt();
                }
            }
        }
    }

    /**
     * Stops the currently running scan.
     * 
     * @return true if a scan was actually running and was interrupted, false otherwise.
     */
    public boolean stopScan()
        throws AutoinventoryException {
        boolean wasScanRunning = false;
        synchronized (scannerList) {
            if (activeScanner != null) {
                activeScanner.stop();
                if (mainThread != null) {
                    mainThread.interrupt();
                }
                wasScanRunning = true;
            }
        }

        long startWait = System.currentTimeMillis();
        while (scanInProgress) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
            if (System.currentTimeMillis() - startWait > 60000) {
                throw new AutoinventoryException("Scan did not stop "
                            + "within 1 minute: " + activeScanner);
            }
        }

        return wasScanRunning;
    }

    /**
     * Get the status of the currently running scan.
     * 
     * @return A ScanState object representing the current status of the running scan.
     */
    public ScanState getStatus() {
        synchronized (scannerList) {
            if (activeScanner != null) {
                return activeScanner.getScanState();
            }
        }
        return null;
    }

    /**
     * @return true if there is a scan currently running, false otherwise.
     */
    public boolean isScanRunning() {
        return (activeScanner != null);
    }

    public boolean isScanQueued() {
        return this.scannerList.size() != 0;
    }

    public void scanComplete(ScanState state)
        throws AutoinventoryException,
        SystemException {
        this.listener.scanComplete(state);
    }
}
