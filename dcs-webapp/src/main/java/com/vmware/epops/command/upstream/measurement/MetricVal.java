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

package com.vmware.epops.command.upstream.measurement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Single metric instance received from hyperic agent MeasurementReport message
 * 
 * @author rina
 */
public class MetricVal {
    private long timestamp;
    private double metricValue;

    public MetricVal() {

    }

    public MetricVal(double value,
                     long timestamp) {
        this.timestamp = timestamp;
        this.metricValue = value;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getMetricValue() {
        return metricValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof MetricVal) {
            MetricVal o = (MetricVal) obj;
            EqualsBuilder equalsBuider = new EqualsBuilder();
            equalsBuider.append(this.getTimestamp(), o.getTimestamp());
            equalsBuider.append(this.getMetricValue(), o.getMetricValue());
            return equalsBuider.isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(timestamp);
        hcb.append(metricValue);
        return hcb.toHashCode();
    }

    @Override
    public String toString() {
        return "MetricVal [timestamp=" + timestamp + ", value=" + metricValue + "]";
    }
}
