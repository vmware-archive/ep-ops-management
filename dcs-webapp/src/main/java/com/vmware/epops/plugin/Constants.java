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

package com.vmware.epops.plugin;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

public class Constants {

    public static final String OS_LINUX = "Linux";
    public static final String OS_WIN32 = "Win32";
    public static final String AGENT_RESOURCE_TYPE = AppdefEntityConstants.HQ_AGENT_PROTOTYPE_NAME;
    public static final String TYPE = "type";
    public static final String TYPE_DESC = "Platform Type";
    public static final String NAME = "name";
    public static final String NAME_DESC = "Name";
    public static final String FQDN = "fqdn";
    public static final String FQDN_DESC = "Fully Qualified Domain Name";
    public static final String DESCRIPTION = "description";
    public static final String DESCRIPTION_DESC = "Description";
    public static final String NETWORK_NAME = "network_config";
    public static final String NETWORK_DESC = "Network Configurations [IP Address, Netmask, Mac Address]";
    public static final String INSTALL_PATH_NAME = "installpath";
    public static final String INSTALL_PATH_DESC = "Install Path";

}
