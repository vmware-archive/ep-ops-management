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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.client.AgentCommandsCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;

/**
 * The client that polls the server for data, dispatches to the server side bean then send the responses to the server.
 */
public class PollerClientImpl implements PollerClient {

    private final AgentCommandsCallbackClient agentCommandsClient;

    private final Map<Class<?>, Object> serviceInterfaceName2ServiceInterface = new HashMap<Class<?>, Object>();

    private final ExecutorService invokersExecutor;

    private final ScheduledExecutorService scheduler;

    private final long frequency;

    public PollerClientImpl(AgentConfig config,
                            AgentStorageProvider storageProvider,
                            long frequency,
                            int asyncThreadPoolSize) {

        agentCommandsClient =
                    new AgentCommandsCallbackClient(new StorageProviderFetcher(storageProvider), config);
        scheduler = Executors.newScheduledThreadPool(1);
        invokersExecutor = Executors.newFixedThreadPool(asyncThreadPoolSize);
        this.frequency = frequency;
    }

    /**
     * @see org.hyperic.hq.transport.PollerClient#start()
     */
    public void start() {
        scheduler.scheduleAtFixedRate(new PollingRunnable(), frequency, frequency, TimeUnit.MILLISECONDS);
    }

    /**
     * @see org.hyperic.hq.transport.PollerClient#stop()
     */
    public void stop()
        throws InterruptedException {
        scheduler.shutdown();
        invokersExecutor.shutdown();
    }

    public void registerService(Class<?> serviceInterface,
                                Object serviceImpl) {
        serviceInterfaceName2ServiceInterface.put(serviceInterface, serviceImpl);
    }

    private class PollingRunnable implements Runnable {

        private final Log _log = LogFactory.getLog(PollingRunnable.class);
        private final LinkedBlockingQueue<InvocationResponse> responseQueue;

        public PollingRunnable() {
            responseQueue = new LinkedBlockingQueue<InvocationResponse>();
        }

        public void run() {
            try {
                doPolling();
            } catch (Throwable t) {
                _log.error(t.getMessage());
                if (_log.isDebugEnabled()) {
                    _log.debug(t.getStackTrace());
                }
            }
        }

        private void doPolling()
            throws Throwable {
            List<InvocationRequest> invocations = agentCommandsClient.getAgentCommands();
            if (_log.isDebugEnabled()) {
                _log.debug("Got invocation requests from the server '" + invocations + "'");
            }
            if (!invocations.isEmpty()) {
                dispatchInvocationRequests(invocations, responseQueue);
            }

            List<InvocationResponse> responses = new ArrayList<InvocationResponse>();
            responseQueue.drainTo(responses);

            if (!responses.isEmpty()) {
                if (!agentCommandsClient.sendAgentResponses(responses)) {
                    _log.error("A problem has occured while sending these responses to the server: " + responses);
                } else if (_log.isDebugEnabled()) {
                    _log.debug("Successfully sent these responses to the server: " + responses);
                }
            }
        }

        private void dispatchInvocationRequests(List<InvocationRequest> invocations,
                                                final LinkedBlockingQueue<InvocationResponse> responseQueue)
            throws Throwable {
            final CountDownLatch doneSignal = new CountDownLatch(invocations.size());
            for (final InvocationRequest invocation : invocations) {
                invokersExecutor.submit(new Runnable() {
                    public void run() {
                        Object service = serviceInterfaceName2ServiceInterface.get(invocation.getServiceInterface());
                        try {
                            Method method =
                                        service.getClass().getMethod(invocation.getMethod(),
                                                    invocation.getParameterTypes());
                            Object result = method.invoke(service, invocation.getArgs());
                            InvocationResponse response = new InvocationResponse(invocation.getSessionId(), result);
                            responseQueue.put(response);
                        } catch (Throwable t) {
                            _log.error(t, t);
                        }
                        doneSignal.countDown();
                    }
                });
            }
            // Wait for all the invocation requests to finish
            doneSignal.await();
        }

    }

}
