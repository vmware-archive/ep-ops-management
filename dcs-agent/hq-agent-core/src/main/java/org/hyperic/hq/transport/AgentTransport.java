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

package org.hyperic.hq.transport;

import java.lang.reflect.Constructor;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.transport.util.TransportUtils;

/**
 * The transport for the HQ agent. Services hosted by this transport should be registered before the transport is
 * started.
 */
public class AgentTransport {

    private final Object lock = new Object();

    private final PollerClient pollerClient;

    private boolean stopped;

    public AgentTransport(AgentConfig config,
                          AgentStorageProvider storageProvider,
                          long pollingFrequency,
                          int asyncThreadPoolSize)
        throws Exception {

        pollerClient = createPollerClient(config, storageProvider, pollingFrequency, asyncThreadPoolSize);
    }

    /**
     * Create the poller client. A ClassNotFoundException is thrown if this is a .ORG instance. The unidirectional
     * transport that requires the poller client is only supported in EE.
     */
    private PollerClient createPollerClient(AgentConfig config,
                                            AgentStorageProvider storageProvider,
                                            long pollingFrequency,
                                            int i)
        throws ClassNotFoundException, Exception {
        Class<?> clazz;

        try {
            clazz = TransportUtils.tryLoadUnidirectionalTransportPollerClient();
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                        "Unidirectional transport is not available in .ORG");
        }

        Constructor<?> constructor = clazz.getConstructor(
                    new Class[] { AgentConfig.class, AgentStorageProvider.class, Long.TYPE, Integer.TYPE });

        return (PollerClient) constructor.newInstance(
                    new Object[] { config, storageProvider, pollingFrequency, i });
    }

    public void registerService(Class<?> serviceInterface,
                                Object serviceImpl) {
        pollerClient.registerService(serviceInterface, serviceImpl);
    }

    /**
     * Start the transport.
     * 
     * @throws Exception
     */
    public void start()
        throws Exception {
        if (isStopped()) {
            return;
        }
        pollerClient.start();
    }

    /**
     * Stop the transport. Once stopped, it cannot be started again.
     */
    public void stop()
        throws InterruptedException {
        pollerClient.stop();
        setStopped();
    }

    private void setStopped() {
        synchronized (lock) {
            stopped = true;
        }
    }

    private boolean isStopped() {
        synchronized (lock) {
            return stopped;
        }
    }

    private static class BootStrapService {
        void doNothing() {
            // no-op
        }
    }

}
