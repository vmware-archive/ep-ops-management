package org.hyperic.plugin.vsphere.sso;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.Relationship.RelationshipUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The results for running the CLI of the SSO's lookup service.
 * {@link org.hyperic.plugin.vsphere.sso.LookupServiceResultTest}
 * Created by bharel on 4/2/2015.
 */
public class LookupServiceResult {

    private static final Map<String, String> serviceTypeToName;
    private static final String hint6x = "serviceVersion";
    private static final String vCenterServerType = "vCenter App Server";
    private static final String vSphereWebClientType = "vSphere Web Client";

    static {
        serviceTypeToName = new HashMap<String, String>(4);
        serviceTypeToName.put("urn:sso:sts", "vSphere SSO");
        serviceTypeToName.put("urn:vc", vCenterServerType);
        serviceTypeToName.put("urn:com.vmware.vsphere.client", vSphereWebClientType);
        serviceTypeToName.put("urn:logbrowser:logbrowser", "vSphere Log Browser");
    }

    private static final Log log = LogFactory.getLog(LookupServiceResult.class);

    private static final String SERVICE_SEPARATOR_5x = "-----------";
    private static final String SERVICE_SEPARATOR_6x = "-------------------------------------------------------";
    private static final String TYPE = "type";
    private static final String ENDPOINTS = "endpoints";

    private Map<String, Set<String>> typeToFqdns;

    public LookupServiceResult(String lookupServiceOutput) {
        if (StringUtils.isBlank(lookupServiceOutput)) {
            return;
        }
        typeToFqdns = new HashMap<String, Set<String>>();

        if (is60LookupServiceOutput(lookupServiceOutput)) {
            create60GetTypeToFqdns(lookupServiceOutput);
            typeToFqdns.put(vSphereWebClientType, new HashSet<String>());
            typeToFqdns.get(vSphereWebClientType).add(RelationshipUtils.getLocalFqdn());
        } else{
            create5xGetTypeToFqdns(lookupServiceOutput);
        }
    }

    public Map<String, Set<String>> getTypeToFqdns() {
        return typeToFqdns;
    }

    private void create60GetTypeToFqdns(String lookupServiceOutput) {
        String[] parts = lookupServiceOutput.split(SERVICE_SEPARATOR_6x);
        for (String part : parts) {
            try {
                Properties properties = getProperties(part);

                for (Map.Entry<Object, Object> keyValuePair : properties.entrySet()) {
                    log.debug(String.format("[create60GetTypeToFqdns] '%s'='%s'", keyValuePair.getKey(), keyValuePair.getValue()));
                    if (((String) keyValuePair.getValue()).equalsIgnoreCase("com.vmware.vim.vcenter.instanceName")) {
                        String vCenterFqdnKey = getVCenterFqdnKey(keyValuePair);
                        addFqdn(properties, vCenterFqdnKey);
                    }
                }
            } catch (IOException e) {
                log.error("failed to find service", e);
            }
        }
    }

    private String getVCenterFqdnKey(Map.Entry<Object, Object> keyValuePair) {
        String key = (String) keyValuePair.getKey();
        return key.substring(0, key.length() - "key".length()) + "value";
    }

    private void addFqdn(Properties properties, String vCenterNameKey) {
        if (!typeToFqdns.containsKey(vCenterServerType)) {
            typeToFqdns.put(vCenterServerType, new HashSet<String>());
        }

        String fqdn = RelationshipUtils.getFqdn(properties.getProperty(vCenterNameKey));
        typeToFqdns.get(vCenterServerType).add(fqdn);
        log.debug(String.format("[addFqdn] Added '%s' to '%s'", fqdn, vCenterServerType));
    }

    private boolean is60LookupServiceOutput(String lookupServiceOutput) {
        return lookupServiceOutput.contains(hint6x);
    }

    private void create5xGetTypeToFqdns(String lookupServiceOutput) {
        String[] parts = lookupServiceOutput.split(SERVICE_SEPARATOR_5x);

        //This starts from 1 as there is junk at the start.
        for (int i = 1; i < parts.length; i++) {
            try {
                Properties properties = getProperties(parts[i]);

                String serviceType = properties.getProperty(TYPE);
                if (serviceTypeToName.containsKey(serviceType)) {
                    String serviceName = serviceTypeToName.get(serviceType);
                    Set<String> fqdns = RelationshipUtils.getFqdns(
                                properties.getProperty(ENDPOINTS), "\\[url=(.*?)\\]");
                    if(typeToFqdns.containsKey(serviceName)) {
                    	typeToFqdns.get(serviceName).addAll(fqdns);
                    } else {
                    	typeToFqdns.put(serviceName, fqdns);
                    }
                    log.debug(String.format("Added '%s' to '%s'", fqdns, vCenterServerType));
                }
            } catch (IOException e) {
                log.error("failed to find service", e);
            }
        }
    }

    private Properties getProperties(String lookupServiceOutput)
                throws IOException {
        Properties properties = new Properties();
        InputStream stream = new ByteArrayInputStream(lookupServiceOutput.getBytes("UTF-8"));
        properties.load(stream);
        return properties;
    }
}
