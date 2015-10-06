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

package com.vmware.epops.plugin.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Liat
 */

public class PluginResourceType {
    private String name;
    private boolean virtual;
    private boolean light;
    private boolean remote;
    private boolean roaming;

    private Set<String> parents = new HashSet<>();

    private List<ResourceTypeConfig> configProperties = new ArrayList<>();
    private List<ResourceTypeMeasurement> measurementProperties = new ArrayList<>();
    private List<ResourceTypeConfig> discoverableProperties = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public boolean isLight() {
        return light;
    }

    public void setLight(boolean light) {
        this.light = light;
    }

    public boolean isRoaming() {
        return roaming;
    }

    public void setRoaming(boolean roaming) {
        this.roaming = roaming;
    }

    public Set<String> getParents() {
        return parents;
    }

    public void setParents(Set<String> parents) {
        this.parents = parents;
    }

    public List<ResourceTypeConfig> getConfigProperties() {
        return configProperties;
    }

    public void setConfigProperties(List<ResourceTypeConfig> configProperties) {
        this.configProperties = configProperties;
    }

    public void setMeasurementProperties(List<ResourceTypeMeasurement> measurementProperties) {
        this.measurementProperties = measurementProperties;
    }

    public List<ResourceTypeMeasurement> getMeasurementProperties() {
        return measurementProperties;
    }

    public void setDiscoverableProperties(List<ResourceTypeConfig> discoverableProperties) {
        this.discoverableProperties = discoverableProperties;
    }

    public List<ResourceTypeConfig> getDiscoverableProperties() {
        return discoverableProperties;
    }

}
