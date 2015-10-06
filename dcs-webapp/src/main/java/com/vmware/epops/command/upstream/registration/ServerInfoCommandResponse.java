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

package com.vmware.epops.command.upstream.registration;

import com.vmware.epops.command.upstream.AgentCommandResponseBase;

public class ServerInfoCommandResponse extends AgentCommandResponseBase {

    private static final long serialVersionUID = 7988287565917461523L;

    private boolean customCertificate;

    public boolean isCustomCertificate() {
        return customCertificate;
    }

    public void setCustomCertificate(boolean customCertificate) {
        this.customCertificate = customCertificate;
    }

}
