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

import org.hyperic.lather.LatherValue;

public class ServerInfo_result extends LatherValue {

    private static final String PROP_IS_CUSTOM_CERIFICATE = "isCustomCertificate";

    public ServerInfo_result() {
        super();
    }

    public void setIsCustomCertificate(String isCustomCertificate) {
        this.setStringValue(PROP_IS_CUSTOM_CERIFICATE, isCustomCertificate);
    }

    public boolean getIsCustomCertificate() {
        return Boolean.parseBoolean(this.getStringValue(PROP_IS_CUSTOM_CERIFICATE));
    }

}
