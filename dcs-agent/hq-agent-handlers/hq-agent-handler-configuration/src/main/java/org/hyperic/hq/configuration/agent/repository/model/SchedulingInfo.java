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

package org.hyperic.hq.configuration.agent.repository.model;

import org.apache.commons.lang.builder.ToStringBuilder;

public class SchedulingInfo {

    private String metricName;
    private int metricId;
    private long pollingInterval;

    public SchedulingInfo(String metricName,
                          int metricId,
                          long pollingInterval) {
        this.metricName = metricName;
        this.metricId = metricId;
        this.pollingInterval = pollingInterval;
    }

    public String getMetricName() {
        return metricName;
    }

    public int getMetricId() {
        return metricId;
    }

    public long getPollingInterval() {
        return pollingInterval;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SchedulingInfo)) {
            return false;
        }
        SchedulingInfo other = (SchedulingInfo) obj;
        return other.metricName.equals(metricName) &&
                    other.metricId == metricId &&
                    other.pollingInterval == pollingInterval;
    }

    @Override
    public int hashCode() {
        return metricName.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                    append("metricName", metricName).
                    append("metricId", metricId).
                    append("pollingInterval", pollingInterval).
                    toString();
    }
}
