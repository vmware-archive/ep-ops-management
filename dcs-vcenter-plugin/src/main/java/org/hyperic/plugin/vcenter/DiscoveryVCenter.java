package org.hyperic.plugin.vcenter;

import static org.hyperic.util.Relationship.RelationshipConstants.RELATIONSHIP_MODEL_PROPERTY;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.Relationship.RelationshipModelWrapper;
import org.hyperic.util.Relationship.RelationshipUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;
import org.w3c.dom.Document;

public class DiscoveryVCenter extends Discovery {

    // 1:DBType 2:HostIPV6 3:Host 4:port 5:db name
    protected static final String JDBC_REGEX = "^jdbc:([^:]*):[^\\/@]*\\/{0,2}@?\\/{0,2}(\\[[^\\]]*\\])?([^\\/:\\\\]*):?(\\d{0,})[:\\/\\\\]{0,2}(.*)$";
    private static final int JDBC_REGEX_DB_TYPE = 1;
    private static final int JDBC_REGEX_HOST_IPV6 = 2;
    private static final int JDBC_REGEX_HOST = 3;
    private static final int JDBC_REGEX_PORT = 4;

    private static final Log log = LogFactory.getLog(DiscoveryVCenter.class);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
            throws PluginException {
        log.debug(String.format("[getServerResources] platformConfig = '%s'", platformConfig));
        @SuppressWarnings("unchecked")
        List<ServerResource> servers = super.getServerResources(platformConfig);

        for (ServerResource server : servers) {
            String query = getProcessQuery();
            if (query != null) {
                ConfigResponse conf = new ConfigResponse(server.getProductConfig().getConfig());
                conf.setValue("process.query", query);
                setProductConfig(server, conf);
            }
        }

        return servers;
    }

    @Override
    protected List<ServiceResource> discoverServices(ConfigResponse config)
            throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List<ServiceResource> res = super.discoverServices(config);

        final String platformFqdn = config.getValue("platform.fqdn");
        RelationshipUtils.setLocalFqdn(platformFqdn);

        // vSphere Profile-Driven Storage
        String vspdds = getExtensionHealthInfo(config,
                "/mob/?moid=ExtensionManager&doPath=extensionList%5b%22com%2evmware%2evim%2esps%22%5d%2ehealthInfo",
                "//*[local-name()='dataObject']/*[local-name()='url']/node()");

        // vCenter Inventory Service
        String vcis = getExtensionHealthInfo(config,
                "/mob/?moid=ExtensionManager&doPath=extensionList%5b%22com%2evmware%2evim%2einventoryservice%22%5d%2ehealthInfo",
                "//*[local-name()='dataObject']/*[local-name()='url']/node()");

        // vSphere Auto Deploy
        String vsad = getExtensionHealthInfo(config,
                "/mob/?moid=ExtensionManager&doPath=extensionList%5b%22com%2evmware%2erbd%22%5d%2ehealthInfo",
                "//*[local-name()='dataObject']/*[local-name()='url']/node()");

        /*
         // vSphere Update Manager Service
         String vsums = getExtensionHealthInfo(config,
         "/mob/?moid=ExtensionManager&doPath=extensionList%5b%22com%2evmware%2evcIntegrity%22%5d%2eclient",
         "//*[local-name()='dataObject']/*[local-name()='url']/node()");
         */

        String jdbcUrl = getJdbcConnectionString();
        JdbcConnectionMetadata jdbcMetadata = parseJdbcUrl(jdbcUrl);
        String dbHost = jdbcMetadata.getDatabaseHost();
        VcDatabaseType dbType = jdbcMetadata.getDbType();

        log.debug("[discoverServices] vspdds=" + vspdds);
        log.debug("[discoverServices] vcis=" + vcis);
        log.debug("[discoverServices] vsad=" + vsad);

        RelationshipModelWrapper relation = new RelationshipModelWrapper();
        relation.put("vCenter Management WebServices", RelationshipUtils.getLocalFqdn());
        relation.put("vCenter ADAM Directory", RelationshipUtils.getLocalFqdn());
        relation.put("vCenter App Server", RelationshipUtils.getLocalFqdn());

        if (dbHost != null) {
            relation.put(String.format("vCenter Database [%s]", dbType), dbHost);
        }

        if (vspdds != null) {
            relation.put("vSphere Profile-Driven Storage", RelationshipUtils.getFqdn(vspdds));
        }
        if (vcis != null) {
            relation.put("vCenter Inventory Service", RelationshipUtils.getFqdn(vcis));
        }
        if (vsad != null) {
            relation.put("vSphere Auto Deploy", RelationshipUtils.getFqdn(vsad));
        }
        //relation.put("vSphere Update Manager Service", vsums);

        ConfigResponse custom = new ConfigResponse();
        custom.setValue(RELATIONSHIP_MODEL_PROPERTY, relation.toString());

        for (ServiceResource service : res) {
            if (service.getName().equals("vSphere Managed Object Browser")) {
                service.setCustomProperties(custom);
                break;
            }
        }

        return res;
    }


    private static String getJdbcConnectionString() {
        String jdbcUrl = StringUtils.EMPTY;

        File configFile;

        if (isWin32()) {
            // Look for configuration in vCenter 6.0 path first
            configFile =
                        new File(System.getenv("ProgramData"),
                                    "VMware\\vCenterServer\\cfg\\vmware-vpx\\vcdb.properties");
            if (!configFile.exists()) {
                log.debug("[getJdbcConnectionString] Database configuration file for vCenter 6.0 was not detected, "
                            + "continue checking for 5.x versions");
                // Look for configuration in vCenter 5.x path
                configFile = new File(System.getenv("ProgramData"), "VMware\\VMware VirtualCenter\\vcdb.properties");
            }
        } else {
            configFile = new File("/etc/vmware-vpx/vcdb.properties");
        }

        if (!configFile.exists()) {
            log.error(String.format("[getJdbcConnectionString] Database configuration file was not found at %s.",
                        configFile.getAbsolutePath()));
        } else {
            FileInputStream in = null;
            Properties properties = new Properties();
            try {
                in = new FileInputStream(configFile);
                properties.load(in);

                jdbcUrl = properties.getProperty("url");

            } catch (IOException ex) {
                log.debug("[getJdbcConnectionString] Error: " + ex.getMessage(), ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        log.debug("[getJdbcConnectionString] Error on file.close : " + ex.getMessage(), ex);
                    }
                }
            }
        }

        return jdbcUrl;
    }


    public static JdbcConnectionMetadata parseJdbcUrl(final String jdbcUrl){
        JdbcConnectionMetadata jdbcMetadata = new JdbcConnectionMetadata();
        Matcher m = Pattern.compile(JDBC_REGEX).matcher(jdbcUrl);
        if (m.find()) {
            int groupsCount = m.groupCount();
            for (int i = 1; i < groupsCount; i++){
                switch (i) {
                    case JDBC_REGEX_DB_TYPE:
                        jdbcMetadata.setDbType(VcDatabaseType.MSSQL.getDatabaseType(m.group(JDBC_REGEX_DB_TYPE)));
                        break;
                    case JDBC_REGEX_HOST:
                        jdbcMetadata.setDbHost(RelationshipUtils.getFqdn(m.group(JDBC_REGEX_HOST)));
                        break;
                    case JDBC_REGEX_HOST_IPV6:
                        jdbcMetadata.setDbHostIpV6(RelationshipUtils.getFqdn(m.group(JDBC_REGEX_HOST_IPV6)));
                        break;
                    case JDBC_REGEX_PORT:
                        jdbcMetadata.setDbPort(m.group(JDBC_REGEX_PORT));
                        break;
                    default:
                        break;
                }
            }
        }
        return jdbcMetadata;
    }


    private static String getExtensionHealthInfo(ConfigResponse config, String path, String xPath) {
        String res = null;

        String user = config.getValue("vcenter.http.user", "");
        String pass = config.getValue("vcenter.http.pass", "");

        String html = queryVCenter(user, pass, path);

        if (html != null) {
            String xml = trimNoXML(html);

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = (Document) builder.parse(new ByteArrayInputStream(xml.getBytes()));

                log.debug("[getExtensionHealthInfo] xml:" + xml);

                XPathFactory xFactory = XPathFactory.newInstance();
                XPath xpath = xFactory.newXPath();

                log.debug("[getExtensionHealthInfo] evaluating XPath:" + xPath);

                res = xpath.evaluate(xPath, doc);

                log.debug("[getExtensionHealthInfo] vcoFNQ:" + res);

            } catch (Exception ex) {
                log.debug("[getExtensionHealthInfo] " + ex, ex);
            }
        }

        return res;
    }

    private static String queryVCenter(String user, String pass, String path) {
        String html = null;
        if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
            log.error("[queryVCenter] no VCenter credentials.");
        } else {
            try {
                AgentKeystoreConfig ksCFG = new AgentKeystoreConfig();
                HQHttpClient client
                        = new HQHttpClient(ksCFG, new HttpConfig(5000, 5000, null, 0), ksCFG.isAcceptUnverifiedCert());

                HttpHost targetHost = new HttpHost("localhost", 443, "https");
                client.getCredentialsProvider().setCredentials(
                        new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                        new UsernamePasswordCredentials(user, pass));

                HttpGet get = new HttpGet(targetHost.toURI() + path);
                log.debug("[queryVCenter] GET: " + get.getURI());
                HttpResponse response = client.execute(get);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    log.debug("[queryVCenter] GET OK (" + statusCode + ")");
                    html = readInputString(response.getEntity().getContent());
                } else {
                    log.debug("[queryVCenter] GET failed: (" + statusCode + ") " + response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException ex) {
                log.debug("[queryVCenter] " + ex, ex);
            }
        }
        return html;
    }

    public static String readInputString(InputStream in)
            throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    private static String trimNoXML(String xml) {
        xml = xml.substring(xml.indexOf("<xml"));
        xml = xml.substring(0, xml.indexOf("</xml>") + "</xml>".length());
        return xml.trim();
    }

    public enum VcDatabaseType {
        MSSQL("sqlserver"),
        POSTGRES("postgresql"),
        ORACLE("oracle");

        private String name;

        VcDatabaseType(String dbTypeName) {
            this.name = dbTypeName;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return this.name.toLowerCase(Locale.ENGLISH);
        }

        public VcDatabaseType getDatabaseType(final String dbType) {
            for (VcDatabaseType type : VcDatabaseType.values()) {
                if (type.name.equalsIgnoreCase(dbType)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Illegal database type: " + dbType);
        }
    }

    static class JdbcConnectionMetadata{
        private VcDatabaseType dbType;
        private String dbHost;
        private String dbHostIpV6;
        private String dbPort;

        public VcDatabaseType getDbType() {
            return dbType;
        }
        public void setDbType(VcDatabaseType dbType) {
            this.dbType = dbType;
        }
        public String getDbHost() {
            return dbHost;
        }
        public void setDbHost(String dbHost) {
            this.dbHost = dbHost;
        }
        String getDbHostIpV6() {
            return dbHostIpV6;
        }
        public void setDbHostIpV6(String dbHostIpV6) {
            this.dbHostIpV6 = dbHostIpV6;
        }
        String getDbPort() {
            return dbPort;
        }
        public void setDbPort(String dbPort) {
            this.dbPort = dbPort;
        }
        public String getDatabaseHost() {
            String databaseHost = null;
            if (StringUtils.isNotEmpty(this.getDbHost())){
                databaseHost = this.getDbHost();
            }else if (StringUtils.isNotEmpty(this.getDbHostIpV6())){
                databaseHost = this.getDbHostIpV6();
            }
            return databaseHost;
        }

        @Override
        public String toString() {
            return String.format("JdbcConnectionMetadata [dbType=%s, dbHost=%s, dbHostIpV6=%s, dbPort=%s]",
                        dbType, dbHost, dbHostIpV6, dbPort);
        }
    }
}
