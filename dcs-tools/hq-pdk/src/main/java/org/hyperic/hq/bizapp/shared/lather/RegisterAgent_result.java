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

import org.apache.commons.lang.StringUtils;
import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

public class RegisterAgent_result
            extends LatherValue {
    private static final String PROP_CERTIFICATE = "certificate";
    private static final String PROP_ERROR_MESSAGE = "errorMessage";
    private static final int MAX_CERTIFICATE_LENGTH = 30000;

    public RegisterAgent_result() {
        super();
    }

    public void setCertificate(String certificate) {
        this.setStringValue(PROP_CERTIFICATE, certificate);
    }

    public String getCertificate() {
        try {
            return this.getStringValue(PROP_CERTIFICATE);
        } catch (LatherKeyNotFoundException exc) {
            return StringUtils.EMPTY;
        }
    }

    public void setErrorMessage(String errorMessage) {
        this.setStringValue(PROP_ERROR_MESSAGE, errorMessage);
    }

    public String getErrorMessage() {
        try {
            return this.getStringValue(PROP_ERROR_MESSAGE);
        } catch (LatherKeyNotFoundException exc) {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public void validate()
        throws LatherRemoteException {
        if (getCertificate().length() > MAX_CERTIFICATE_LENGTH) {
            throw new LatherRemoteException("Certificate is too long. Please contact the administrator");
        }
    }
}
