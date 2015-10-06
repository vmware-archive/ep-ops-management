package org.hyperic.util.Relationship;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by bharel on 4/2/2015.
 */
public class RelationshipUtilsTest {

    private final String exampleFqdn = "example.com";

    @Before
    public void before() {
        RelationshipUtils.setLocalFqdn(exampleFqdn);
    }

    @Test
    public void testGetFQDN() {
        ArrayList<String> addresses = new ArrayList<String>() {
            {
                add(exampleFqdn);
                add("example.com:7444");
                add("example.com\\:7444");
                add("https://example.com/component-registry/");
                add("https\\://example.com/component-registry/");
                add("sqlserver://example.com:1433/vCO;domain=refarch.eng.vmware.com;useNTLMv2=true");
                add("example.com:1433/vCO");
            }
        };
        for (String address : addresses) {
            String fqdn = RelationshipUtils.getFqdn(address);
            Assert.assertEquals("tried the address: " + address, exampleFqdn, fqdn);
        }
    }

    @Test
    public void testGetFqdnMallformedAddress() {
        ArrayList<String> malformedAddresses = new ArrayList<String>() {
            {
                add(" ");
                add("");
                add(null);
            }
        };
        for (String address : malformedAddresses) {
            String fqdn = RelationshipUtils.getFqdn(address);
            Assert.assertEquals("tried the address: " + address, exampleFqdn, fqdn);
        }
    }

    @Test
    public void testGetFqdnWithIp() {
        ArrayList<String> addresses = new ArrayList<String>() {
            {
                add("localhost");
                add("127.0.0.1");
                add("http://127.0.0.1");
                add("127.0.0.1:80");
                add("http://127.0.0.1:80");
                add("127.0.0.1:800");
                add("http://127.0.0.1:800");
            }
        };

        for (String address : addresses) {
            String fqdn = RelationshipUtils.getFqdn(address);
            Assert.assertEquals(String.format("tried the address: %s", address), exampleFqdn, fqdn);
        }

    }

    @Test
    public void testGetFqdnWithIpv6() {
        ArrayList<String> addresses = new ArrayList<String>();
        addresses.add("localhost");
        addresses.add("::1");
        addresses.add("0000:0000:0000:0000:0000:0000:0000:0001");
        addresses.add("0000::0001");
        addresses.add("[::1]");
        addresses.add("[0000:0000:0000:0000:0000:0000:0000:0001]");
        addresses.add("[0000::0001]");
        addresses.add("http://[::1]");
        addresses.add("http://[0000:0000:0000:0000:0000:0000:0000:0001]");
        addresses.add("http://[0000::0001]");
        addresses.add("[::1]:80");
        addresses.add("[0000:0000:0000:0000:0000:0000:0000:0001]:80");
        addresses.add("[0000::0001]:80");
        addresses.add("http://[::1]:80");
        addresses.add("http://[0000:0000:0000:0000:0000:0000:0000:0001]:80");
        addresses.add("http://[0000::0001]:80");

        for (String address : addresses) {
            String fqdn = RelationshipUtils.getFqdn(address);
            Assert.assertEquals(String.format("tried the address: %s", address), exampleFqdn, fqdn);
        }
    }
}
