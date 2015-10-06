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

package org.hyperic.hq.common;

import java.io.Serializable;
import java.util.Arrays;

public class InvocationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Class<?> serviceInterface;
    private String method;
    private Object[] args;
    private Class<?>[] parameterTypes;
    private String sessionId;

    public InvocationRequest() {
    }

    public InvocationRequest(Class<?> serviceInterface,
                             String method,
                             Object[] args,
                             Class<?>[] parameterTypes,
                             String sessionId) {
        this.serviceInterface = serviceInterface;
        this.method = method;
        this.args = args;
        this.parameterTypes = parameterTypes;
        this.sessionId = sessionId;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * @param sessionId - Test
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "InvocationRequest [serviceInterface=" + serviceInterface
                    + ", method=" + method + ", args=" + Arrays.toString(args)
                    + ", parameterTypes=" + Arrays.toString(parameterTypes)
                    + ", sessionId=" + sessionId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(args);
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + Arrays.hashCode(parameterTypes);
        result = prime
                    * result
                    + ((serviceInterface == null) ? 0 : serviceInterface.hashCode());
        result = prime * result
                    + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InvocationRequest other = (InvocationRequest) obj;
        if (!Arrays.equals(args, other.args))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        if (!Arrays.equals(parameterTypes, other.parameterTypes))
            return false;
        if (serviceInterface == null) {
            if (other.serviceInterface != null)
                return false;
        } else if (!serviceInterface.equals(other.serviceInterface))
            return false;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        } else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }

}
