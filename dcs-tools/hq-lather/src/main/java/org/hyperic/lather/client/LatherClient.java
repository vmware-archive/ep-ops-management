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

package org.hyperic.lather.client;

import java.io.IOException;

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

/**
 * The LatherClient is an interface which clients (such as an HTTP client, etc.) implement.
 */
public interface LatherClient {
    /**
     * Invoke a remote method. Connection would be close after the call.
     * 
     * @param method Name of the method to invoke
     * @param args The arguments for the method
     * 
     * @return a LatherValue return value from the server, which is an instantiated subclass, as dictated by the server.
     */
    public LatherValue invoke(String method,
                              LatherValue args)
        throws IOException, LatherRemoteException;

    /**
     * Invoke a remote method. Connection will be closed if closeConn is set to true, o.w. false.
     * 
     * @param method Name of the method to invoke
     * @param args The arguments for the method
     * 
     * @return a LatherValue return value from the server, which is an instantiated subclass, as dictated by the server.
     */
    public LatherValue invoke(String method,
                              LatherValue args,
                              boolean closeConn)
        throws IOException, LatherRemoteException;

}
