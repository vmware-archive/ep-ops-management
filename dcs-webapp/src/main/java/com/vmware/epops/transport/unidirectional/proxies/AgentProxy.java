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

package com.vmware.epops.transport.unidirectional.proxies;

import org.hyperic.hq.common.InvocationRequest;

@SuppressWarnings("rawtypes")
public class AgentProxy {

    private final Class serviceInterface;

    public AgentProxy(Class serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    // for no args commands
    public InvocationRequest getInvocationRequest(String methodName)
        throws Throwable { // NOPMD
        return getInvocationRequest(methodName, null, null);
    }

    public InvocationRequest getInvocationRequest(String methodName,
                                                  Class<?>[] parameterTypes,
                                                  Object[] args)
        throws Throwable { // NOPMD
        InvocationRequest request = new InvocationRequest();
        request.setArgs(args);
        request.setMethod(methodName);
        request.setParameterTypes(parameterTypes);
        request.setServiceInterface(serviceInterface);
        return request;
    }

}
