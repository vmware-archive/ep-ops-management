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

package com.vmware.epops.command.downstream.mail.configuration;

public class MetricScheduling {

    private int metricId;
    private String metricName;
    private Long pollingInterval;

    public MetricScheduling() {

    }

    public MetricScheduling(int metricId,
                            String metricName,
                            Long pollingInterval) {
        this.metricId = metricId;
        this.metricName = metricName;
        this.pollingInterval = pollingInterval;
    }

    public int getMetricId() {
        return metricId;
    }

    public String getMetricName() {
        return metricName;
    }

    public Long getPollingInterval() {
        return pollingInterval;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public void setPollingInterval(Long pollingInterval) {
        this.pollingInterval = pollingInterval;
    }
}
