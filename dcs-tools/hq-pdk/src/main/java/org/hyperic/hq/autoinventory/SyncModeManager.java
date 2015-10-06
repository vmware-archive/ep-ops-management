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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStorageProvider;

public class SyncModeManager {
    private static final String NEXT_SYNC_TIME = "nextSyncScanTime";

    private static Log _log = LogFactory.getLog(SyncModeManager.class);

    private final AtomicBoolean syncMode = new AtomicBoolean(false);
    private AgentStorageProvider storageProvider;

    // Sync scans interval - currently defined in agent.properties or by default value.
    private long _syncScanInterval;
    private long _nextSyncScanTime;

    public SyncModeManager(AgentStorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    /**
     * Returns true if agent sent a sync AI report, and has not recived yet a push runtime configuration command, or
     * false o.w.
     * 
     * @return true if in stync mode, false o.w.
     */
    public boolean isSyncMode() {
        return syncMode.get();
    }

    /**
     * Sets syncs mode state. Setting to true, implies that the agent sent a sync report and has not yet recived a push
     * runtime configuration command.
     * 
     * @param syncMode - new sync mode state
     */
    public void setSyncMode(boolean syncMode) {
        this.syncMode.set(syncMode);
    }

    /**
     * init the sync scan interval and next sync scan time.
     * 
     * @param loadScanInterval
     * @return true if read next sync scan time from storage, else false.
     */
    public boolean initSyncScanFromStorage(long loadScanInterval) {
        setSyncScanInterval(loadScanInterval);
        return initNextSyncScanTimeFromStorage();
    }

    /**
     * Sets syncs mode state. Setting to true, implies that the agent sent a sync report and has not yet recived a push
     * runtime configuration command.
     * 
     * @param syncMode - new sync mode state
     */
    private void setSyncScanInterval(long loadScanInterval) {
        _syncScanInterval = loadScanInterval;
    }

    /**
     * Normally the first scan is set to now. Special case: a new plugin was deployed on the server. In this case all
     * agents will download it and restart. This will lead to aproximetly same sync time on all agents. To prevent this,
     * we keep the nextSyncScanTime of the agent to storage when downloding the plugins, and after restart loading value
     * form there.(and deleting if from the file)
     * 
     * @return true if read next sync scan time from storage, else false.
     */
    private boolean initNextSyncScanTimeFromStorage() {
        try {
            _nextSyncScanTime = storageProvider.getObject(NEXT_SYNC_TIME);
            storageProvider.deleteObject(NEXT_SYNC_TIME); // clean from the storage
            _log.debug("got nextSyncScanTime from storage ");
            return true;
        } catch (Exception e) {
            // Choose a random time point within interval range for sync scans.
            // uncomment when implement random point choose.
            // Random rand = new Random();
            // long randomSyncPoint = rand.nextLong() % _syncScanInterval;
            _nextSyncScanTime = System.currentTimeMillis(); // default - run now.
        }
        return false;
    }

    public void storeNextSyncScanTime() {
        storageProvider.saveObject(_nextSyncScanTime, NEXT_SYNC_TIME);
        _log.debug("storing nextSyncScanTime in storage ");
    }

    public long getNextSyncScanTime() {
        return _nextSyncScanTime;
    }

    public void tickNextSyncScanTime() {
        _nextSyncScanTime += _syncScanInterval;
    }
}
