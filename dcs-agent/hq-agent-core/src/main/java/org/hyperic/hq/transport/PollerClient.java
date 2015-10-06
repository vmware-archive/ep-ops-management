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

/**
 * The poller client for the unidirectional transport.
 */
public interface PollerClient {

    /**
     * Start the poller client.
     */
    void start();

    /**
     * Stop the poller client. This operation will block until the polling thread dies or 30 seconds.
     * 
     * @throws InterruptedException
     */
    void stop()
        throws InterruptedException;

    /**
     * @param serviceInterface
     * @param serviceImpl
     */
    void registerService(Class<?> serviceInterface,
                         Object serviceImpl);

}
