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

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Liat
 */
public class ResourceTypeConfig {

    private String name;
    private String displayName;
    private String description;
    private String type;
    private boolean mandatory;
    private String defaultValue;
    private boolean enumType = false;
    private List<String> enumValues;
    private boolean password;
    private boolean sensitive;
    private boolean hidden = false;
    private boolean readonly = false;
    private Boolean advanced = null;

    public ResourceTypeConfig() {

    }

    public ResourceTypeConfig(String name,
                              String displayName,
                              String description,
                              String type,
                              boolean mandatory,
                              String defaultValue,
                              boolean readonly,
                              Boolean advanced) {
        super();
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
        this.readonly = readonly;
        this.advanced = advanced;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isMandatory() {
        return this.mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isEnumType() {
        return enumType;
    }

    public void setEnumType(boolean enumType) {
        this.enumType = enumType;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(Boolean advanced) {
        this.advanced = advanced;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ResourceTypeConfig)) {
            return false;
        }
        final ResourceTypeConfig rhs = (ResourceTypeConfig) obj;
        final EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(name, rhs.name);
        equalsBuilder.append(displayName, rhs.displayName);
        equalsBuilder.append(description, rhs.description);
        equalsBuilder.append(type, rhs.type);
        equalsBuilder.append(mandatory, rhs.mandatory);
        equalsBuilder.append(defaultValue, rhs.defaultValue);
        equalsBuilder.append(hidden, rhs.hidden);
        equalsBuilder.append(readonly, rhs.readonly);
        equalsBuilder.append(advanced, rhs.advanced);

        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(name);
        hashCodeBuilder.append(displayName);
        hashCodeBuilder.append(description);
        hashCodeBuilder.append(type);
        hashCodeBuilder.append(mandatory);
        hashCodeBuilder.append(defaultValue);
        hashCodeBuilder.append(hidden);
        hashCodeBuilder.append(readonly);
        hashCodeBuilder.append(advanced);

        return hashCodeBuilder.toHashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append(name);
        toStringBuilder.append(displayName);
        toStringBuilder.append(description);
        toStringBuilder.append(type);
        toStringBuilder.append(mandatory);
        toStringBuilder.append(defaultValue);
        toStringBuilder.append(hidden);
        toStringBuilder.append(readonly);
        toStringBuilder.append(advanced);

        return toStringBuilder.toString();
    }
}
