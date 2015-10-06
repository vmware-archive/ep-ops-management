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

package com.vmware.epops.plugin.converter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.DirConfigOption;
import org.hyperic.util.config.DoubleConfigOption;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.IpAddressConfigOption;
import org.hyperic.util.config.LongConfigOption;
import org.hyperic.util.config.PortConfigOption;
import org.hyperic.util.config.StringArrayConfigOption;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.config.YesNoConfigOption;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.epops.plugin.converter.ConfigOptionConverterFactory;
import com.vmware.epops.plugin.converter.PropertyConfigOption;
import com.vmware.epops.plugin.converter.AbstarctConfigOptionConverter.ResourceIdentifierType;
import com.vmware.epops.plugin.model.ResourceTypeConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class ConfigOptionConverterTest {

    private String optName;
    private String optDesc;
    private String optType;
    @Autowired
    private ConfigOptionConverterFactory configOptionConverterFactory;

    @Before
    public void setup() {
        optName = "Option Name";
        optDesc = "Option Description";
        optType = ResourceIdentifierType.STRING.toString();

    }

    @Test
    public void configOptionTest() {
        String defValue = "Option Default Value";
        StringConfigOption co = new StringConfigOption(optName, optDesc, defValue);
        co.setOptional(false);
        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(ConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

    }

    @Test
    public void stringConfigOptionTest() {
        String defValue = "Option Default Value";
        StringConfigOption co = new StringConfigOption(optName, optDesc, defValue);
        co.setOptional(false);
        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(StringConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

    }

    @Test
    public void booleanConfigOptionTest() {
        boolean defValue = true;
        BooleanConfigOption co = new BooleanConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(BooleanConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, Boolean.toString(defValue), optType, optName, resourceTypeConfig);

        Assert.assertTrue("Configuration Option Enum flag should be Identical",
                    resourceTypeConfig.isEnumType());
        Assert.assertArrayEquals("Configuration Option Enum values should be Identical",
                    Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()).toArray(),
                    resourceTypeConfig.getEnumValues().toArray());

    }

    @Test
    public void yesNoConfigOptionTest() {
        String defValue = YesNoConfigOption.YES;
        YesNoConfigOption co = new YesNoConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(YesNoConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

        Assert.assertTrue("Configuration Option Enum flag should be Identical",
                    resourceTypeConfig.isEnumType());
        Assert.assertArrayEquals("Configuration Option Enum values should be Identical",
                    Arrays.asList(YesNoConfigOption.YES, YesNoConfigOption.NO).toArray(),
                    resourceTypeConfig.getEnumValues().toArray());

    }

    @Test
    public void enumerationConfigOptionTest() {
        String defValue = "enum1";
        String[] enumValues = new String[] { "enum1", "enum2", "enum3", "enum4" };
        EnumerationConfigOption co = new EnumerationConfigOption(optName, optDesc, defValue, enumValues);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(EnumerationConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

        assertEquals("Configuration Option Enum flag should be Identical", true,
                    resourceTypeConfig.isEnumType());
        Assert.assertArrayEquals("Configuration Option Enum values should be Identical", enumValues,
                    resourceTypeConfig.getEnumValues().toArray());

    }

    @Test
    public void integerConfigOptionTest() {
        optType = ResourceIdentifierType.INT.toString();
        Integer defValue = 10;
        IntegerConfigOption co = new IntegerConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(IntegerConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, Integer.toString(defValue), optType, optName, resourceTypeConfig);

    }

    @Test
    public void doubleConfigOptionTest() {

        double defValue = 10.12121;
        DoubleConfigOption co = new DoubleConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(DoubleConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, Double.toString(defValue), optType, optName, resourceTypeConfig);

    }

    @Test
    public void dirConfigOptionTest() {

        String defValue = "/opt/test";
        DirConfigOption co = new DirConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(DirConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

    }

    @Test
    public void portConfigOptionTest() {
        optType = ResourceIdentifierType.INT.toString();
        Integer defValue = 6000;
        PortConfigOption co = new PortConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(PortConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, Integer.toString(defValue), optType, optName, resourceTypeConfig);

    }

    @Test
    public void longConfigOptionTest() {
        long defValue = 6L;
        LongConfigOption co = new LongConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(LongConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, Long.toString(defValue), optType, optName, resourceTypeConfig);

    }

    @Test
    public void ipConfigOptionTest() {
        optType = ResourceIdentifierType.IP.toString();
        String defValue = "10.23.222.22";
        IpAddressConfigOption co = new IpAddressConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(IpAddressConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

    }

    @Test
    public void notFoundConfigOptionTest() {
        String defValue = "String Array";
        StringArrayConfigOption co = new StringArrayConfigOption(optName, optDesc, defValue);
        co.setOptional(false);

        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(StringArrayConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(co);
        assertDefaultAttr(optName, optDesc, defValue, optType, optName, resourceTypeConfig);

    }

    @Test
    public void propertyConfigOptionTest() {
        String defValue = "Option Default Value";
        StringConfigOption co = new StringConfigOption(optName, optDesc, defValue);
        co.setOptional(false);
        PropertyConfigOption propertyConfigOption = new PropertyConfigOption();
        propertyConfigOption.setConfigOption(co);
        // Get Converter
        Converter<ConfigOption, ResourceTypeConfig> converter =
                    configOptionConverterFactory.getConverter(PropertyConfigOption.class);
        // Convert to ResourceTypeConfig
        ResourceTypeConfig resourceTypeConfig = converter.convert(propertyConfigOption);
        assertDefaultAttr(optName, optDesc, defValue, optType, optDesc, resourceTypeConfig);

    }

    private void assertDefaultAttr(String name,
                                   String desc,
                                   String defValue,
                                   String type,
                                   String displayName,
                                   ResourceTypeConfig resourceTypeConfig) {
        assertEquals("Configuration Option Name should be Identical", name, resourceTypeConfig.getName());
        assertEquals("Configuration Option Type should be Identical", type, resourceTypeConfig.getType());
        assertEquals("Configuration Option Description should be Identical", desc,
                    resourceTypeConfig.getDescription());
        assertEquals("Configuration Option Default Value should be Identical", defValue,
                    resourceTypeConfig.getDefaultValue());
        assertEquals("Configuration Option Mandatory flag should be Identical", true,
                    resourceTypeConfig.isMandatory());
        assertEquals("Configuration Option DisplayName should be Identical", displayName,
                    resourceTypeConfig.getDisplayName());
    }
}
