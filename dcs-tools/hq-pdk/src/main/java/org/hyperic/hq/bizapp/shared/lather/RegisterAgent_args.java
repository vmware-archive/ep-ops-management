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

package org.hyperic.hq.bizapp.shared.lather;

import org.apache.log4j.Logger;
import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;

public class RegisterAgent_args
            extends AgentInfo_args
{
    private static final String PROP_CPUCOUNT = "cpuCount";
    private static final String PROP_VERSION = "version";
    private static final String PROP_CSR = "csr";

    private static final String VALIDATE_VERSION_PATTERN = "^[-\\w\\.]+$"; // word chars, hyphen and dots. E.g.
    // 5.8.0-super_beta
    private static int VERSION_LENGTH_MAX = 20;
    private static int CPU_MAX_VALUE = 2048;
    private static int CSR_LENGTH_MAX = 1200;

    private final static Logger LOGGER = Logger.getLogger(RegisterAgent_args.class);

    public RegisterAgent_args() {
        super();
    }

    public void setVersion(String version) {
        this.setStringValue(PROP_VERSION, version);
    }

    public String getVersion() {
        return this.getStringValue(PROP_VERSION);
    }

    public void setCpuCount(int count) {
        this.setIntValue(PROP_CPUCOUNT, count);
    }

    public int getCpuCount() {
        return this.getIntValue(PROP_CPUCOUNT);
    }

    public void setCertificateRequest(byte[] csr) {
        this.setByteAValue(PROP_CSR, csr);
    }

    public byte[] getCertificateRequest() {
        return this.getByteAValue(PROP_CSR);
    }

    @Override
    public void validate()
        throws LatherRemoteException {

        super.validate();

        int cpuCount;
        String version;
        byte[] csr;

        try {
            // Checks if values exist (are not null)
            cpuCount = this.getCpuCount();
            version = this.getVersion();
            csr = this.getCertificateRequest();
        } catch (LatherKeyNotFoundException exc) {
            throw new LatherRemoteException("Not all values were set. First value that wasn't set is:"
                        + exc.getMessage());
        }
        // Check if values are valid
        // Note: checking for string overflow, by validating its length is positive
        if (csr.length > CSR_LENGTH_MAX || csr.length < 0) {
            LOGGER.error("Bad lather value denied: bad csr length; didn't match expected. Length was: "
                        + csr.length + ", expected maximum length of: " + CSR_LENGTH_MAX);
            throw new LatherRemoteException(INVALID_VALUE_ERROR);
        }

        if (version.length() > VERSION_LENGTH_MAX || version.length() < 0) {
            LOGGER.error("Bad lather value denied: version field length didn't match expected. Length was: "
                        + version.length() + " expected maximum of: " + VERSION_LENGTH_MAX);
            throw new LatherRemoteException(INVALID_VALUE_ERROR);
        }

        if (!version.matches(VALIDATE_VERSION_PATTERN)) {
            // Outputting actual value, as length was validated before this line (and length is minimal)
            LOGGER.error("Bad lather value denied: version field pattern didn't match expected: " + version);
            throw new LatherRemoteException(INVALID_VALUE_ERROR);
        }

        if (cpuCount > CPU_MAX_VALUE || cpuCount < 0) {
            LOGGER.error("Bad lather value denied: cpuCount was " + cpuCount + ", expected: 0.." + CPU_MAX_VALUE);
            throw new LatherRemoteException(INVALID_VALUE_ERROR);
        }
    }
}
