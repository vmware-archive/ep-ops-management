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

/**
 *
 */
package com.vmware.epops.plugin.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Liat
 */
public class ResourceTypeMeasurement implements Comparable<ResourceTypeMeasurement> {

    private String name;
    private String key;
    private String group;
    private String units;
    private boolean defaultMonitored = false;
    private boolean indicator = false;
    private boolean discrete = false;

    public ResourceTypeMeasurement() {
    };

    public ResourceTypeMeasurement(String name,
                                   String key,
                                   String group) {
        super();
        this.name = name;
        this.key = key;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public boolean isDefaultMonitored() {
        return defaultMonitored;
    }

    public void setDefaultMonitored(boolean defaultMonitored) {
        this.defaultMonitored = defaultMonitored;
    }

    public boolean isIndicator() {
        return indicator;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    public boolean isDiscrete() {
        return discrete;
    }

    public void setDiscrete(boolean discrete) {
        this.discrete = discrete;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ResourceTypeMeasurement)) {
            return false;
        }
        final ResourceTypeMeasurement rhm = (ResourceTypeMeasurement) obj;
        final EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(name, rhm.name);
        equalsBuilder.append(key, rhm.key);
        equalsBuilder.append(group, rhm.group);
        equalsBuilder.append(units, rhm.units);
        equalsBuilder.append(defaultMonitored, rhm.defaultMonitored);
        equalsBuilder.append(indicator, rhm.indicator);
        equalsBuilder.append(discrete, rhm.discrete);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(name);
        hashCodeBuilder.append(key);
        hashCodeBuilder.append(group);
        hashCodeBuilder.append(units);
        hashCodeBuilder.append(defaultMonitored);
        hashCodeBuilder.append(indicator);
        hashCodeBuilder.append(discrete);
        return hashCodeBuilder.toHashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append(group);
        toStringBuilder.append(units);
        toStringBuilder.append(name);
        toStringBuilder.append(key);
        toStringBuilder.append(defaultMonitored);
        toStringBuilder.append(indicator);
        toStringBuilder.append(discrete);
        return toStringBuilder.toString();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_CONVERT_CASE",
                justification = "No language other than english exists in the hq-plugin.xml")
    @Override
    public int compareTo(ResourceTypeMeasurement measurment) {
        String fullName1 = this.getGroup().toLowerCase() + this.getName().toLowerCase();
        String fullName2 = measurment.getGroup().toLowerCase() + measurment.getName().toLowerCase();
        return fullName1.compareTo(fullName2);
    }
}
