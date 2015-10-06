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

package org.hyperic.hq.plugin.ntds;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.IntegerConfigOption;

public class ActiveDirectoryDetector
extends ServerDetector
implements RegistryServerDetector {

    private static Log log =
                LogFactory.getLog("ActiveDirectoryDetector");

    private static final String[] SERVICES = {
        "LDAP", "Authentication"
    };

    private static final Map<Integer, String> schemaToServerVersion;

    static {
        Map<Integer, String> schemaToVersionMap = new HashMap<Integer, String>(7);
        schemaToVersionMap.put(30, "2003 RTM");
        schemaToVersionMap.put(31, "2003 R2");
        schemaToVersionMap.put(44, "2008 RTM");
        schemaToVersionMap.put(47, "2008 R2");
        schemaToVersionMap.put(52, "2012");
        schemaToVersionMap.put(56, "2012 RTM");
        schemaToVersionMap.put(69, "2012 R2");
        schemaToServerVersion = schemaToVersionMap;
    }

    public List getServerResources(ConfigResponse platformConfig,
                                   String path,
                                   RegistryKey current)
                                               throws PluginException {

        if (!new File(path).exists()) {
            log.debug(path + " does not exist");
            return null;
        }

        ConfigResponse cprops = new ConfigResponse();
        List cpropKeys = getCustomPropertiesSchema().getOptions();
        for (int i = 0; i < cpropKeys.size(); i++) {
            ConfigOption option = (ConfigOption) cpropKeys.get(i);
            String key = option.getName();
            String value;
            try {
                if (option instanceof IntegerConfigOption) {
                    value = String.valueOf(current.getIntValue(key));
                }
                else {
                    value = current.getStringValue(key).trim();
                }
            } catch (Win32Exception e) {
                continue;
            }
            cprops.setValue(key, value);
        }

        // we use the AD schema version to decide which server plugin to use, because of a bug in sigar which causes it
        // to identify win 2012 as win 2008
        int schemaVersion = -1;
        try {
            schemaVersion = Integer.valueOf(cprops.getValue("Schema Version"));
        } catch (NumberFormatException e) {
            log.error("Failed to discover AD schema version", e);
            return null;
        }
        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue("version", schemaToServerVersion.get(schemaVersion));
        cprops.setValue("Server Version", schemaToServerVersion.get(schemaVersion));
        ServerResource server = createServerResource(path);
        server.setProductConfig(productConfig);
        server.setMeasurementConfig();
        server.setCustomProperties(cprops);
        final String platformDisplayName = platformConfig.getValue(ProductPlugin.PROP_PLATFORM_DISPLAY_NAME);
        server.setName(prepareName(getTypeInfo().getName(), platformDisplayName));
        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }

    private String prepareName(String typeName, String platformDisplayName) {
        String serverName;
        if(platformDisplayName == null) {
            serverName = typeName;
        }
        else {
            serverName =
                        String.format("%s" + HQConstants.RESOURCE_NAME_DELIM + "%s", typeName, platformDisplayName);
        }
        return serverName;
    }

    protected List discoverServices(ConfigResponse serverConfig)
                throws PluginException {

        List services = new ArrayList();

        for (int i = 0; i < SERVICES.length; i++) {
            ServiceResource service = new ServiceResource();
            service.setType(this, SERVICES[i]);
            service.setServiceName(SERVICES[i]);
            service.setProductConfig();
            service.setMeasurementConfig();
            services.add(service);
        }

        return services;
    }
}
