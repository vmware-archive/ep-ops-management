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

package com.vmware.epops.model;

import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * class contains 3 sets of plugins hashes that represent the diff of a given report from the server.
 * 
 */
public class PluginReportDiff {

    private final Set<String> added;
    private final Set<String> modified;
    private final Set<String> removed;

    public PluginReportDiff(Set<String> added,
                            Set<String> modified,
                            Set<String> removed) {
        this.added = added;
        this.modified = modified;
        this.removed = removed;
    }

    public Set<String> getAdded() {
        return added;
    }

    public Set<String> getModified() {
        return modified;
    }

    public Set<String> getRemoved() {
        return removed;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PluginReportDiff)) {
            return false;
        }
        final PluginReportDiff other = (PluginReportDiff) obj;
        final EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(added, other.added);
        equalsBuilder.append(modified, other.modified);
        equalsBuilder.append(removed, other.removed);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(added);
        hashCodeBuilder.append(modified);
        hashCodeBuilder.append(removed);
        return hashCodeBuilder.toHashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder toStringBuilder = new ToStringBuilder(this,
                    ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append(added);
        toStringBuilder.append(modified);
        toStringBuilder.append(removed);
        return toStringBuilder.toString();
    }
}
