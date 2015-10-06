/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.product;

import java.util.Map;

import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

//wrapper class so plugins do not directly deal with AIServiceValue

public class ServiceResource {
    public static final String SERVER_NAME_PLACEHOLDER = "%serviceName%";

    // package has access to this field
    AIServiceValue resource;

    public ServiceResource() {
        this.resource = new AIServiceValue();
    }

    public void setServiceName(String name) {
        setName(name);
    }

    public void setType(GenericPlugin plugin,
                        String type) {
        setType(plugin.getTypeInfo().getName() + " " + type);
    }

    // following methods are the same as ServiceResource's
    public void setType(String name) {
        this.resource.setServiceTypeName(name);
    }

    public String getType() {
        return this.resource.getServiceTypeName();
    }

    public void setName(String name) {
        this.resource.setName(name);
    }

    public String getName() {
        return this.resource.getName();
    }

    public void setDescription(String description) {
        this.resource.setDescription(description);
    }

    public String getDescription() {
        return this.resource.getDescription();
    }

    private RuntimeException encodeException() {
        return new RuntimeException("Error encoding config");
    }

    public void setProductConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setProductConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public ConfigResponse getProductConfig() {
        try {
            return ConfigResponse.decode(this.resource.getProductConfig());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setMeasurementConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setMeasurementConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setMeasurementConfig(ConfigResponse config,
                                     int logTrackLevel) {
        LogTrackPlugin.setEnabled(config,
                    TypeInfo.TYPE_SERVICE,
                    logTrackLevel);
        setMeasurementConfig(config);
    }

    public void setControlConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setControlConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setResponseTimeConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setResponseTimeConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setProductConfig() {
        this.resource.setProductConfig(ConfigResponse.EMPTY_CONFIG);
    }

    public void setMeasurementConfig() {
        this.resource.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
    }

    public void setControlConfig() {
        this.resource.setControlConfig(ConfigResponse.EMPTY_CONFIG);
    }

    public void setCustomProperties(ConfigResponse config) {
        if ((config == null) || (config.getKeys().size() == 0)) {
            return;
        }
        try {
            this.resource.setCustomProperties(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setProductConfig(Map config) {
        setProductConfig(new ConfigResponse(config));
    }

    public void setMeasurementConfig(Map config) {
        setMeasurementConfig(new ConfigResponse(config));
    }

    public void setControlConfig(Map config) {
        setControlConfig(new ConfigResponse(config));
    }

    public void setCustomProperties(Map props) {
        setCustomProperties(new ConfigResponse(props));
    }

    public String toString() {
        return this.resource.toString();
    }
}
