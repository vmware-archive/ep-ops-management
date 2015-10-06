package org.hyperic.plugin.vsphere.sso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.Relationship.RelationshipModelWrapper;
import org.hyperic.util.Relationship.RelationshipUtils;
import org.hyperic.util.config.ConfigResponse;

import static org.hyperic.util.Relationship.RelationshipConstants.*;

import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

/**
 * @author imakhlin
 */
public class DiscoveryVSphereSSO extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVSphereSSO.class);
    private static final String SSO_DOMAIN = "vSphere SSO Domain";
    private final String programFiles = System.getenv("ProgramFiles");
    private final String ssoCli55 = String.format("%s\\VMware\\Infrastructure\\VMware\\CIS\\vmware-sso\\ssolscli.cmd",
                programFiles);
    private final String ssoCli51 = String.format("%s\\VMware\\Infrastructure\\SSOServer\\ssolscli\\ssolscli.cmd",
                programFiles);
    private String ssoLoadBalancerFilePath;
    private SsoVersion ssoVersion;

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
                throws PluginException {

        log.debug(String.format("[getServerResources] platformConfig = '%s'", platformConfig));
        List<ServerResource> servers = super.getServerResources(platformConfig);

        for (ServerResource server : servers) {
            String query = getProcessQuery();
            if (query != null) {
                ConfigResponse conf = new ConfigResponse(server.getProductConfig().getConfig());
                conf.setValue("process.query", query);
                setProductConfig(server, conf);
            }
        }

        setRelatedResources(servers);

        return servers;
    }

    public String[] get51JavaHomeEnv() {
        return new String[]{String.format("JAVA_HOME=%s\\VMware\\Infrastructure\\jre", programFiles)};
    }

    @Override
    protected List<ServiceResource> discoverServices(ConfigResponse config)
                throws PluginException {

        List<ServiceResource> res = new ArrayList<ServiceResource>();
        TypeInfo[] types = this.getPluginData().getTypes();

        for (TypeInfo type : types) {
            if (type.getType() != TypeInfo.TYPE_SERVICE) {
                continue;
            }
            if (!type.getName().startsWith(getTypeInfo().getName())) {
                continue;
            }
            if (!this.getTypeInfo().getVersion().equals(type.getVersion())) {
                continue;
            }

            log.debug("[discoverServices] '" + type.getName() + "' on '" + getTypeInfo().getName() + "'");
            boolean valid = true;

            String query = getServiceProcessQuery(type);
            if (query != null) {
                log.debug("[discoverServices] PROC_QUERY='" + query + "'");
                long[] pids = getPids(query);
                log.debug("[discoverServices] Found '" + ((pids != null) ? pids.length : 0) + "' process");
                if ((pids == null) || (pids.length == 0)) {
                    valid = false;
                }
            }

            String urlPAth = getTypeProperty(type.getName(), "URL_PATH");
            if (urlPAth != null) {
                String html = queryLocalHTTP(urlPAth);
                valid = (html != null);
            }

            if (valid) {
                ServiceResource service = new ServiceResource();
                service.setType(type.getName());
                String name = getTypeNameProperty(type.getName());
                service.setServiceName(name);
                ConfigResponse conf = new ConfigResponse();
                if (query != null) {
                    conf.setValue("process.query", query);
                }
                setProductConfig(service, conf);
                setMeasurementConfig(service, new ConfigResponse());
                res.add(service);
                log.debug("[discoverServices] '" + type.getName() + "' created");
            } else {
                log.debug("[discoverServices] '" + type.getName() + "' ignored");
            }
        }

        return res;
    }

    protected String getServiceProcessQuery(TypeInfo type) {
        String res = null;
        if (isWin32()) {
            res = getTypeProperty(type.getName(), "WIN32_PROC_QUERY");
        }
        if (res == null) {
            res = getTypeProperty(type.getName(), "PROC_QUERY");
        }
        return res;
    }

    private static String queryLocalHTTP(String path) {
        String html = null;
        try {
            AgentKeystoreConfig ksCFG = new AgentKeystoreConfig();
            HQHttpClient client =
                        new HQHttpClient(ksCFG, new HttpConfig(5000, 5000, null, 0), ksCFG.isAcceptUnverifiedCert());
            HttpHost targetHost = new HttpHost("localhost", 7444, "https");
            HttpGet get = new HttpGet(targetHost.toURI() + path);
            log.debug("[queryLocalHTTP] GET: " + get.getURI());
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                log.debug("[queryLocalHTTP] GET OK (" + statusCode + ")");
                html = readInputString(response.getEntity().getContent());
            } else {
                log.debug("[queryLocalHTTP] GET failed: (" + statusCode + ") "
                            + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException ex) {
            log.debug("[queryLocalHTTP] " + ex, ex);
        }
        return html;
    }

    public static String readInputString(InputStream in)
                throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    private RelationshipModelWrapper getRelatedResources() {
        RelationshipModelWrapper relationshipModelWrapper = new RelationshipModelWrapper();
        relationshipModelWrapper.put("vSphere SSO", RelationshipUtils.getLocalFqdn());

        mapIfValueNotBlank(relationshipModelWrapper, TYPE_VSPHERE_SSO_LOAD_BALANCER, getSsoLoadBalancerFqdn());
        mapIfValueNotBlank(relationshipModelWrapper, SSO_DOMAIN, getSsoDomain());

        LookupServiceResult lookupServiceResult = new LookupServiceResult(getLookupServiceOutput());
        Map<String, Set<String>> typeToFqdns = lookupServiceResult.getTypeToFqdns();

        if (typeToFqdns != null) {
            relationshipModelWrapper.putAll(typeToFqdns);
        }

        return relationshipModelWrapper;
    }

    private void setRelatedResources(List<ServerResource> servers) {
        RelationshipModelWrapper relationshipModelWrapper = getRelatedResources();

        if (relationshipModelWrapper == null || StringUtils.isBlank(relationshipModelWrapper.toString())) {
            return;
        }

        String relationshipModel = relationshipModelWrapper.toString();

        for (ServerResource server : servers) {
            server.setCustomProperty(RELATIONSHIP_MODEL_PROPERTY, relationshipModel);
        }
    }

    private void mapIfValueNotBlank(
                RelationshipModelWrapper relationshipModelWrapper,
                String key, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        relationshipModelWrapper.put(key, value);
    }

    private String getSsoLoadBalancerFqdn() {
        String ssoLoadBalancerFqdn = RelationshipUtils.readFile(getFileWithSsoLoadBalancerAddress());
        if (ssoLoadBalancerFqdn == null) {
            return null;
        }

        ssoLoadBalancerFqdn = RelationshipUtils.getFqdn(ssoLoadBalancerFqdn.trim());
        return ssoLoadBalancerFqdn;
    }

    private String getSsoDomain() {
        String res = StringUtils.EMPTY;
        String kerbos5FileContent = getKerbos5FileContent();
        if (StringUtils.isNotEmpty(kerbos5FileContent)) {
            Pattern pattern = Pattern.compile("\\n\\s*default_realm\\s*=\\s*(.*)");
            Matcher matcher = pattern.matcher(kerbos5FileContent);
            if (matcher.find()) {
                res = matcher.group(1);
            }
        }
        log.debug("[getSsoDomain] res: '" + res + "'");
        return res;
    }

    private String getLookupServiceOutput() {
        String[] command = getSsoLookupServiceCommand();
        log.debug("[getLookupServiceOutput] command = " + Arrays.asList(command));
        String result;
        if (isSetJavaHomeNeeded()) {
            result = RelationshipUtils.runCommand(command, get51JavaHomeEnv());
        } else {
            result = RelationshipUtils.runCommand(command);
        }
        log.debug("[getLookupServiceOutput] result.length = " + result.length());
        return result;
    }

    private boolean isSetJavaHomeNeeded() {
        return ssoVersion == SsoVersion.SSO_51 && isWin32() && StringUtils.isBlank(System.getenv("JAVA_HOME"));
    }

    private String getFileWithSsoLoadBalancerAddress() {
        if (ssoLoadBalancerFilePath == null) {
            if (isWin32()) {
                ssoLoadBalancerFilePath =
                            String.format("%s\\VMware\\CIS\\cfg\\vmware-sso\\hostname.txt",
                                        System.getenv("ProgramData"));
            } else {
                ssoLoadBalancerFilePath = getLinuxHostnameFilePath();
            }
        }

        return ssoLoadBalancerFilePath;
    }

    private String getKerbos5FileContent() {
        String kerbos5FilePath = getKerbos5FilePath();
        String res = RelationshipUtils.readFile(kerbos5FilePath);
        log.debug("[getKerbos5FileContent] res: '" + res + "'");
        return res;
    }

    private String[] getSsoLookupServiceCommand() {
        if (isWin32()) {
            return getWindowsLookupServiceCommand();
        }

        return getLinuxLookupServiceCommand();
    }

    private String getLinuxHostnameFilePath() {
        String ssoLbPath = "/etc/vmware-sso/hostname.txt";
        File hostNameFile = new File(ssoLbPath);
        if (hostNameFile.exists() && !hostNameFile.isDirectory()) {
            return ssoLbPath;
        }

        return "/etc/vmware-identify/hostname.txt";
    }

    private String getKerbos5FilePath() {
        String res = "/etc/krb5.lotus.conf";
        if (isWin32()) {
            res = String.format("%s\\MIT\\Kerberos5\\krb5.ini", System.getenv("ProgramData"));
            if (!isFileExists(res)) {
                res = String.format("%s\\MIT\\Kerberos5\\krb.ini", System.getenv("ProgramData"));
            }
        }
        log.debug("[getKerbos5FilePath] path: '" + res + "' " + (isFileExists(res) ? "OK" : "KO"));
        return res;
    }

    private String[] getWindowsLookupServiceCommand() {
        setSsoVersion();

        switch (ssoVersion) {
            case SSO_6X:
                return get6xCommand();
            case SSO_51:
                return get5xCommand(ssoCli51);
            case SSO_55:
                return get5xCommand(ssoCli55);
            default:
                throw new IllegalArgumentException(
                            String.format("Failed to get lookup service command, ssoVersion: '%s'", ssoVersion));
        }
    }

    private String[] getLinuxLookupServiceCommand() {
        String vi_regtool5x = "/usr/lib/vmware-sso/bin/vi_regtool";
        if (isFileExists(vi_regtool5x)) {
            return get5xCommand(vi_regtool5x);
        }
        return get6xCommand(new String[] { "/usr/lib/vmidentity/tools/scripts/lstool.py" });
    }

    private boolean isFileExists(String path) {
        return new File(path).exists();
    }

    private void setSsoVersion() {
        if (isFileExists(ssoCli51)) {
            ssoVersion = SsoVersion.SSO_51;
            return;
        }

        if (isFileExists(ssoCli55)) {
            ssoVersion = SsoVersion.SSO_55;
            return;
        }

        ssoVersion = SsoVersion.SSO_6X;
    }

    private String[] get5xCommand(String cliPath) {
        return new String[] {
                    cliPath
                    , "listServices"
                    , getLookupServiceAddress() };
    }

    private String[] get6xCommand() {
        String vCenterHomePath = String.format("%s\\VMware\\vCenter Server", programFiles);
        String pythonPath = String.format("\"%s\\python\\python.exe\"", vCenterHomePath);
        String[] ssoLookupServiceExecutable = {
                    pythonPath
                    , String.format("\"%s\\VMware Identity Services\\lstool\\scripts\\lstool.py\"",
                    vCenterHomePath) };
        return get6xCommand(ssoLookupServiceExecutable);
    }

    private String[] get6xCommand(String[] ssoLookupServiceExecutable) {
        String[] lookupService6xParams = new String[] {
                    "list"
                    , "--url"
                    , getLookupServiceAddress()
                    , "--type"
                    , "vcenterserver"
                    , "--no-check-cert"
                    , "--as-spec"
                    , "--ep-proto"
                    , "http"
        };

        return (String[]) ArrayUtils.addAll(ssoLookupServiceExecutable, lookupService6xParams);
    }

    private String getLookupServiceAddress() {
        return String.format("https://%s:7444/lookupservice/sdk", RelationshipUtils.getLocalFqdn());
    }

}
