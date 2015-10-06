/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vcenter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hyperic.plugin.vcenter.DiscoveryVCenter.JdbcConnectionMetadata;
import org.hyperic.util.Relationship.RelationshipUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author glaullon
 */
public class DiscoveryVCenterTest {

    private final String vcExampleFqdn = "vcenter.example.com";

    @Before
    public void initialize(){
       RelationshipUtils.setLocalFqdn(vcExampleFqdn);
    }

    @Test
    public void testParseLookupServiceOutput() {
        String tests[][] = {
            {"jdbc:oracle:thin:@//localhost:1521/XE", vcExampleFqdn},
            {"jdbc:oracle:thin:@neptune.acme.com:1521:T10A", "neptune.acme.com"},
            {"jdbc:oracle:thin:@127.0.0.1:1521:T10A", vcExampleFqdn},
            {"jdbc:postgresql://host:1234/database", "host"},
            {"jdbc:sqlserver://DVC-1-MSSQL-28K", "DVC-1-MSSQL-28K"},
            {"jdbc:oracle:thin:@[::1]:1521:T10A", vcExampleFqdn},
            {"jdbc:postgresql://[1111:2222:3333:4444:5555:6666:7777:8888]:1232/mysql", vcExampleFqdn},
            {"jdbc:sqlserver://[1111:2222:3333:4444:5555:6666:7777:8888]/mysql", vcExampleFqdn}
        };

        for (String[] test : tests) {
            Matcher m = Pattern.compile(DiscoveryVCenter.JDBC_REGEX).matcher(test[0]);
            Assert.assertTrue(m.find());
            Assert.assertEquals(test[1], DiscoveryVCenter.parseJdbcUrl(test[0]).getDatabaseHost());
        }
    }

    @Test
    public void testSQLServerParser(){
        final String url = String.format("jdbc:sqlserver://%s\\\\VIM_SQLEXP;databaseName\\=VIM_VCDB;integratedSecurity\\=true", vcExampleFqdn);
        JdbcConnectionMetadata metadata =  DiscoveryVCenter.parseJdbcUrl(url);
        Assert.assertNotNull(metadata);
        Assert.assertEquals(vcExampleFqdn, metadata.getDbHost());
        Assert.assertEquals("", metadata.getDbPort());
        Assert.assertEquals("sqlserver", metadata.getDbType().toString());
    }

    @Test
    public void testSQLServerParserLocal(){
        final String url = "jdbc:sqlserver://localhost\\\\VIM_SQLEXP;databaseName\\=VIM_VCDB;integratedSecurity\\=true";
        JdbcConnectionMetadata metadata =  DiscoveryVCenter.parseJdbcUrl(url);
        Assert.assertNotNull(metadata);
        Assert.assertEquals(vcExampleFqdn, metadata.getDbHost());
        Assert.assertEquals("", metadata.getDbPort());
        Assert.assertEquals("sqlserver", metadata.getDbType().toString());
    }

    @Test
    public void testPostgresParser(){
        final String url = String.format("jdbc:postgresql://%s:5432/VCDB", vcExampleFqdn);
        JdbcConnectionMetadata metadata =  DiscoveryVCenter.parseJdbcUrl(url);
        Assert.assertNotNull(metadata);
        Assert.assertEquals(vcExampleFqdn, metadata.getDbHost());
        Assert.assertEquals("5432", metadata.getDbPort());
        Assert.assertEquals("postgresql", metadata.getDbType().toString());
    }

    @Test
    public void testPostgresParserLocal(){
        final String url = "jdbc:postgresql://localhost:5432/VCDB";
        JdbcConnectionMetadata metadata =  DiscoveryVCenter.parseJdbcUrl(url);
        Assert.assertNotNull(metadata);
        Assert.assertEquals(vcExampleFqdn, metadata.getDbHost());
        Assert.assertEquals("5432", metadata.getDbPort());
        Assert.assertEquals("postgresql", metadata.getDbType().toString());
    }
}
