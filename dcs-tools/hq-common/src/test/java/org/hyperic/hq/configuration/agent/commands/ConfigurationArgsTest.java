package org.hyperic.hq.configuration.agent.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests Configuration_args API for setting/getting configuration message arguments.
 */
public class ConfigurationArgsTest {

    /**
     * Tests resource kind name setter and getter.
     */
    @Test
    public void testResourceKind() {
        Configuration_args args = new Configuration_args();
        args.setResourceKind("resouceKind");
        Assert.assertEquals("resouceKind", args.getResourceKind());
    }

    /**
     * Tests resource internal id setter and getter.
     */
    @Test
    public void testResourceInternalId() {
        Configuration_args args = new Configuration_args();
        args.setResourceInternalId(1001);
        Assert.assertEquals("1001", args.getResourceInternalId());
    }

    /**
     * Tests resource parent id setter and getter.
     */
    @Test
    public void testParentId() {
        Configuration_args args = new Configuration_args();
        args.setParentId("parentId");
        Assert.assertEquals("parentId", args.getParentId());
    }

    /**
     * Tests resource monitored resource id setter and getter.
     */
    @Test
    public void testMonitoredResourceId() {
        Configuration_args args = new Configuration_args();
        args.setMonitoredResourceId("monitoredResourceId");
        Assert.assertEquals("monitoredResourceId", args.getMonitoredResourceId());
    }

    /**
     * Tests resource configuration setter and getter. The configuration is prefixed with some contents, this test
     * ensures that the prefix is removed when using the getter.
     */
    @Test
    public void testConfiguration() {
        Map<String, String> config = createMockMap(100);
        Configuration_args args = new Configuration_args();
        args.setConfiguration(config);
        Assert.assertEquals(config, args.getConfiguration());
    }

    /**
     * Tests resource secured configuration setter and getter. The configuration is prefixed with some contents, this
     * test ensures that the prefix is removed when using the getter.
     */
    @Test
    public void testSecuredConfiguration() {
        Map<String, String> config = createMockMap(100);
        Configuration_args args = new Configuration_args();
        args.setSecuredConfiguration(config);
        Assert.assertEquals(config, args.getSecuredConfiguration());
    }

    /**
     * Tests resource scgeduling setter and getter. The scheduling is a triplet prefixed with a serial number of the
     * schedule. This test, tests if a given set of scheduling is stored and returned correctly.
     */
    @Test
    public void testSchedulings() {
        List<String[]> schedulings = createMockScheduling();
        Configuration_args args = new Configuration_args();
        for (String[] schedule : schedulings) {
            args.addScheduling(schedule[2], schedule[0], schedule[1]);
        }
        List<String[]> resSchedulings = args.getSchedulings();
        Collections.sort(resSchedulings, new Comparator<String[]>() {
            public int compare(String[] o1,
                               String[] o2) {
                return o1[0].compareTo(o2[0]);
            }
        });
        for (int i = 0; i < schedulings.size(); i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(schedulings.get(i)[j], resSchedulings.get(i)[j]);
            }
        }
    }

    private List<String[]> createMockScheduling() {
        List<String[]> schedulings = new ArrayList<String[]>(90);
        for (int i = 10; i < 99; i++) {
            schedulings.add(new String[] { "Id_" + i, "Polling_" + i, "Name_" + i });
        }
        return schedulings;
    }

    private Map<String, String> createMockMap(int size) {
        Map<String, String> mock = new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            mock.put("Key_" + i, "Value_" + i);
        }
        return mock;
    }

}
