/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.measurement.agent.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentNotificationHandler;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.AgentTransportLifecycle;
import org.hyperic.hq.agent.server.CollectorThread;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.measurement.agent.MeasurementCommandsAPI;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_args;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_result;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.measurement.agent.commands.SetProperties_args;
import org.hyperic.hq.measurement.agent.commands.SetProperties_result;
import org.hyperic.hq.measurement.agent.commands.TrackPluginAdd_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginAdd_result;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_result;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurementsById_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.UnscheduleTopn_result;
import org.hyperic.hq.measurement.agent.server.ScheduleThread.ParsedTemplate;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;

public class MeasurementCommandsServer
            implements AgentServerHandler, AgentNotificationHandler {
    private static final long THREAD_JOIN_WAIT = 10 * 1000;

    private final MeasurementCommandsAPI verAPI; // Common API specifics
    private Thread scheduleThread; // Thread of scheduler
    private ScheduleThread scheduleObject; // Our scheduler
    private Thread senderThread; // Thread of sender
    private SenderThread senderObject; // Our sender
    private AgentStorageProvider storage; // Agent storage
    private final Map validProps; // Map of valid props
    private AgentConfig bootConfig; // Agent boot config
    private MeasurementSchedule schedStorage; // Schedule storage
    private final Log log; // Our log

    // Config and Log track
    private Thread trackerThread; // Config and Log tracker thread
    private TrackerThread trackerObject; // Config and Log tracker object

    private TopNScheduler topnScheduler;

    private MeasurementCommandsService measurementCommandsService;

    private CollectorThread collectorThread;

    private SchedulerOffsetManager schedulerOffsetManager;

    public MeasurementCommandsServer() {
        this.verAPI = new MeasurementCommandsAPI();
        this.scheduleThread = null;
        this.scheduleObject = null;
        this.senderThread = null;
        this.senderObject = null;
        this.storage = null;
        this.validProps = Collections.synchronizedMap(new HashMap());
        this.bootConfig = null;
        this.schedStorage = null;
        this.log = LogFactory.getLog(MeasurementCommandsServer.class);

        this.trackerThread = null;
        this.trackerObject = null;

        this.topnScheduler = null;

        for (String element : this.verAPI.propSet) {
            // Simply setup true object values for properties we know about
            this.validProps.put(element, this);
        }
    }

    private void spawnThreads(SenderThread senderObject,
                              ScheduleThread scheduleObject,
                              TrackerThread trackerObject)
        throws AgentStartException
    {
        this.senderThread = new Thread(senderObject, "SenderThread");
        senderThread.setDaemon(true);
        this.scheduleThread = new Thread(scheduleObject, "ScheduleThread");
        scheduleThread.setDaemon(true);
        this.trackerThread = new Thread(trackerObject, "TrackerThread");
        this.trackerThread.setDaemon(true);
        this.collectorThread = CollectorThread.getInstance(AgentDaemon.getMainInstance().getMeasurementPluginManager());

        this.senderThread.start();
        this.scheduleThread.start();
        this.trackerThread.start();
        this.collectorThread.doStart();
    }

    public AgentAPIInfo getAPIInfo() {
        return this.verAPI;
    }

    public String[] getCommandSet() {
        return MeasurementCommandsAPI.commandSet;
    }

    public AgentRemoteValue dispatchCommand(String cmd,
                                            AgentRemoteValue args,
                                            InputStream in,
                                            OutputStream out)
        throws AgentRemoteException
    {
        if (cmd.equals(MeasurementCommandsAPI.command_scheduleMeasurements)) {
            ScheduleMeasurements_args sa =
                        new ScheduleMeasurements_args(args);
            measurementCommandsService.scheduleMeasurements(sa);
            return new ScheduleMeasurements_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_unscheduleMeasurements)) {
            UnscheduleMeasurements_args sa =
                        new UnscheduleMeasurements_args(args);
            measurementCommandsService.unscheduleMeasurements(sa);
            return new UnscheduleMeasurements_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_unscheduleMeasurementsById)) {
            UnscheduleMeasurementsById_args sa =
                        new UnscheduleMeasurementsById_args(args);
            measurementCommandsService.unscheduleMeasurementsById(sa);
            return new UnscheduleMeasurements_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_getMeasurements)) {
            GetMeasurements_args sa =
                        new GetMeasurements_args(args);
            return measurementCommandsService.getMeasurements(sa);
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_setProperties)) {
            SetProperties_args sa =
                        new SetProperties_args(args);
            measurementCommandsService.setProperties(sa);
            return new SetProperties_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_deleteProperties)) {
            DeleteProperties_args sa =
                        new DeleteProperties_args(args);
            measurementCommandsService.deleteProperties(sa);
            return new DeleteProperties_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_trackAdd)) {
            TrackPluginAdd_args ta = new TrackPluginAdd_args(args);
            measurementCommandsService.addTrackPlugin(ta);
            return new TrackPluginAdd_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_trackRemove)) {
            TrackPluginRemove_args ta = new TrackPluginRemove_args(args);
            measurementCommandsService.removeTrackPlugin(ta);
            return new TrackPluginRemove_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_scheduleTopn)) {
            ScheduleTopn_args topnArgs = new ScheduleTopn_args(args);
            measurementCommandsService.scheduleTopn(topnArgs);
            return new TrackPluginRemove_result();
        }

        else if (cmd.equals(MeasurementCommandsAPI.command_unscheduleTopn)) {
            measurementCommandsService.unscheduleTopn();
            return new UnscheduleTopn_result();
        }

        else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    public void startup(AgentDaemon agent)
        throws AgentStartException
    {
        Iterator<ScheduledMeasurement> i = null;

        try {
            this.storage = agent.getStorageProvider();
            this.bootConfig = agent.getBootConfig();
            this.schedStorage = new MeasurementSchedule(this.storage, bootConfig.getBootProperties());
            logMeasurementSchedule(this.schedStorage);
        } catch (AgentRunningException exc) {
            throw new AgentAssertionException("Agent should be running here", exc);
        }

        this.schedulerOffsetManager = new SchedulerOffsetManager(this.storage);

        this.senderObject = new SenderThread(this.bootConfig,
                    this.storage, this.schedStorage,
                    this.bootConfig.getBootProperties(),
                    this.schedulerOffsetManager);

        this.scheduleObject = new ScheduleThread(this.senderObject,
                    this.bootConfig.getBootProperties(),
                    this.schedulerOffsetManager);

        this.trackerObject =
                    new TrackerThread(this.storage,
                                this.bootConfig);

        this.topnScheduler = new TopNScheduler(this.storage, this.bootConfig);

        this.measurementCommandsService = new MeasurementCommandsService(this.storage, this.validProps,
                    this.schedStorage, this.scheduleObject, this.topnScheduler);

        AgentTransportLifecycle agentTransportLifecycle;

        try {
            agentTransportLifecycle = agent.getAgentTransportLifecycle();
        } catch (Exception e) {
            throw new AgentStartException("Unable to get agent transport lifecycle: " +
                        e.getMessage());
        }

        log.info("Registering Measurement Commands Service with Agent Transport");

        try {
            agentTransportLifecycle.registerService(MeasurementCommandsClient.class,
                        measurementCommandsService);
        } catch (Exception e) {
            throw new AgentStartException("Failed to register Measurement Commands Service.", e);
        }

        spawnThreads(this.senderObject, this.scheduleObject, this.trackerObject);

        try {
            i = this.schedStorage.getMeasurementList();
        } catch (Exception e) {
            throw new AgentStartException("Failed reading the measurement list from the storage.", e);
        }
        while (i.hasNext()) {
            ScheduledMeasurement meas = i.next();
            this.measurementCommandsService.scheduleMeasurement(meas);
        }

        agent.registerMonitor("camMetric.schedule", this.scheduleObject);
        agent.registerMonitor("camMetric.sender", this.senderObject);

        // If we have don't have a provider, register a handler until
        // we get one
        if (CommandsAPIInfo.getProvider(this.storage) == null) {
            agent.registerNotifyHandler(this,
                        CommandsAPIInfo.NOTIFY_SERVER_SET);
        } else {
            this.startConfigPopulator();
        }

        this.log.info("Measurement Commands Server started up");
    }

    public void handleNotification(String msgClass,
                                   String msg) {
        this.startConfigPopulator();
    }

    private void startConfigPopulator() {
        StorageProviderFetcher fetcher = new StorageProviderFetcher(this.storage);
        MeasurementCallbackClient client = new MeasurementCallbackClient(fetcher, this.bootConfig);
        ConfigPopulateThread populator = new ConfigPopulateThread(client);
        populator.setDaemon(true);
        // We need to re-enable the following line once we support measurementGetConfigs command
        // populator.start();
    }

    private void interruptThread(Thread t)
        throws InterruptedException
    {
        if (t.isAlive()) {
            t.interrupt();
            t.join(THREAD_JOIN_WAIT);

            if (t.isAlive()) {
                this.log.warn(t.getName() + " did not die within the " +
                            "timeout period.  Killing it");
                t.stop();
            }
        }
    }

    private void logMeasurementSchedule(MeasurementSchedule sched) {
        if (this.log.isDebugEnabled()) {
            try {
                Iterator scheduleIter = sched.getMeasurementList();
                int scheduleSize = 0;

                while (scheduleIter.hasNext()) {
                    ScheduledMeasurement metric = (ScheduledMeasurement) scheduleIter.next();
                    if (metric != null) {
                        ParsedTemplate templ = ScheduleThread.getParsedTemplate(metric);
                        scheduleSize++;
                        StringBuffer s = new StringBuffer("Measurement Schedule[")
                                    .append(scheduleSize)
                                    .append("]: entityId=").append(metric.getEntity())
                                    .append(", category=").append(metric.getCategory())
                                    .append(", interval=").append(metric.getInterval())
                                    .append(", derivedId=").append(metric.getDerivedID())
                                    .append(", dsnId=").append(metric.getDsnID())
                                    .append(", dsn=").append(templ.metric.toDebugString());

                        this.log.debug(s.toString());
                    }
                }
                this.log.debug("Measurement schedule list size=" + scheduleSize);
            } catch (Exception e) {
                // since logging the measurement schedule is in debug mode,
                // also log any exceptions in debug mode
                this.log.debug("Could not display measurement schedule: " + e.getMessage(), e);
            }
        }
    }

    public final void postInitActions()
        throws AgentStartException { /*do nothing*/
    }// EOM

    public void shutdown() {
        this.log.info("Measurement Commands Server shutting down");
        logMeasurementSchedule(this.schedStorage);

        this.scheduleObject.die();
        this.collectorThread.doStop();
        this.senderObject.die();
        this.topnScheduler.die();

        try {
            this.interruptThread(this.senderThread);
            this.interruptThread(this.scheduleThread);
        } catch (InterruptedException exc) {
            // Someone wants us to die badly .... ok
            this.log.warn("shutdown interrupted");
        }

        this.log.info("Measurement Commands Server shut down");
    }

    public void refreshOnPluginsChange() {
        collectorThread.refreshOnPluginsChange();
    }
}
