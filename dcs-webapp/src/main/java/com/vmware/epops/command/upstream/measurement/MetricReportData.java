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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.vmware.epops.util.CommonUtils;

/**
 * Single metric with all values (different timestamps)
 * 
 */
public class MetricReportData {
    private final int resourceId;
    private final int metricId;
    private final List<MetricVal> metricValues = new ArrayList<MetricVal>();

    public MetricReportData(long id) {
        super();
        resourceId = CommonUtils.getResourceId(id);
        metricId = CommonUtils.getAttributeKeyId(id);
    }

    public List<MetricVal> getMetricValues() {
        return metricValues;
    }

    public void addMetricValue(MetricVal metricVal) {
        metricValues.add(metricVal);
    }

    public int getResourceId() {
        return resourceId;
    }

    public int getMetricId() {
        return metricId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof MetricReportData) {
            MetricReportData o = (MetricReportData) obj;
            EqualsBuilder equalsBuider = new EqualsBuilder();
            equalsBuider.append(this.getResourceId(), o.getResourceId());
            equalsBuider.append(this.getMetricId(), o.getMetricId());
            equalsBuider.append(this.getMetricValues(), o.getMetricValues());
            return equalsBuider.isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(resourceId);
        hcb.append(metricId);
        hcb.append(metricValues);
        return hcb.toHashCode();
    }

    @Override
    public String toString() {
        return "MetricVals [resourceId=" + resourceId + ",MetricId=" + metricId + ", values=" + metricValues + "]";
    }
}
