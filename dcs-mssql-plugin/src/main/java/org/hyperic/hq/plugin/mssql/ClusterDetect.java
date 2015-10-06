package org.hyperic.hq.plugin.mssql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;

public class ClusterDetect {
    private static final Log log = LogFactory.getLog(ClusterDetect.class);
    private static final Pattern CLUSTER_NAME_PATTERN = Pattern.compile(
                "Listing properties for '(.*)'", Pattern.CASE_INSENSITIVE);

    private static final String CLUSTER_ID_REGKEY = "ClusterInstanceID";
    private static final String CLUSTER_REGKEY = "Cluster";
    private static final String APPCMD = "C:/Windows/System32/cluster.exe";

    private static boolean isCluster = false;


    public static Boolean isCluster() {
        return isCluster;
    }

    private static Map<String, Boolean> instanceOwner = new ConcurrentHashMap<String, Boolean>();

    public static Boolean isOwner(String instanceName) {
        if (instanceName == null) {
            return null;
        }
        return instanceOwner.get(instanceName);
    }

    public static String pollInstanceOwner(String instanceName) {
        if (instanceName == null) {
            return "";
        }
        String ownerName = getInstanceOwner(instanceName);
        if (ownerName != null) {
            ownerName = ownerName.trim();
            String platformName = GenericPlugin.getPlatformName().trim();
            Boolean active = ownerName.equalsIgnoreCase(platformName);
            instanceOwner.put(instanceName, active);
            return active.toString();
        }
        return "";
    }

    /**
     *
     * @param instanceName
     * @return properties of cluster. If can't find clusterId in registry, return null.
     */
    public static ConfigResponse getMssqlClusterProps(String instanceName) {
        ConfigResponse props = new ConfigResponse();
        String clusterName = getClusterName();
        if (clusterName == null) {
            log.debug("Cluster name not found");
            return props;
        }
        log.debug("Cluster name: " + clusterName);

        String clusterResources = runCommand(new String[] { APPCMD, "res", "/priv" });

        log.debug("Cluster resources: " + clusterResources);
        String sqlServerResource = getSqlServerResource(clusterResources,
                instanceName);
        if (sqlServerResource == null) {
            log.debug("SQL server resource is null");
            return props;
        }
        log.debug("SQL server resource: " + sqlServerResource);

        String networkName = getNetworkNameFromResource(sqlServerResource,
                clusterResources);

        if (networkName == null) {
            log.debug("Network name is null");
            return props;
        }
        log.debug("Network name: " + networkName);

        props.setValue(MsSQLDetector.PROP_CLUSTER_NAME, clusterName);
        props.setValue(MsSQLDetector.PROP_NETWORK_NAME, networkName);

        String clusterNodes = getClusterNodes();
        props.setValue(MsSQLDetector.PROP_CLUSTER_NODES, clusterNodes);
        return props;
    }

    private static String getClusterNodes() {
        String clusterNodesOutput = runCommand(new String[] { APPCMD, "node" });
        String result = "";

        Pattern nodeTablePattern = Pattern.compile("(\\-+)\\s+(\\-+)\\s+(\\-+)(.+)", Pattern.DOTALL);
        Matcher matcher = nodeTablePattern.matcher(clusterNodesOutput);
        if(!matcher.find()) {
            return result;
        }

        String table = matcher.group(4);

        Pattern nodeNamePattern = Pattern.compile("^(\\S+)\\s+", Pattern.MULTILINE);
        Matcher nameMatcher = nodeNamePattern.matcher(table);

        boolean isFirst = true;
        while(nameMatcher.find()) {
            if(!isFirst) {
                result += ",";
            }
            result += nameMatcher.group(1);
            isFirst = false;
        }

        return result;
    }

    private static String getSqlServerResource(String clusterResources,
            String instanceName) {
        Pattern sqlServerResourcePattern = Pattern.compile(
                "^(\\w+)\\s+(.+\\S)\\s+InstanceName\\s+" + instanceName,
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = sqlServerResourcePattern.matcher(clusterResources);
        if (!matcher.find()) {
            return null;
        }
        String sqlServerResource = matcher.group(2);
        return sqlServerResource;
    }

    private static String getNetworkNameFromResource(
            String networkNameResource, String clusterResources) {
        Pattern sqlNetworkNamePattern = Pattern.compile(Pattern.quote(networkNameResource)
                + "\\s+VirtualServerName\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = sqlNetworkNamePattern.matcher(clusterResources);
        if (!matcher.find()) {
            return null;
        }
        String networkName = matcher.group(1);
        return networkName;
    }

    private static String getClusterName() {
        String output = runCommand(new String[] { APPCMD, "/prop" });
        if (output == null) {
            return null;
        }
        Matcher matcher = CLUSTER_NAME_PATTERN.matcher(output);
        if (!matcher.find()) {
            return null;
        }
        String clusterName = matcher.group(1);
        return clusterName;
    }

    public static String getClusterId() {
        String clusterId = null;
        RegistryKey clusterKey = null;
        try {
            clusterKey = RegistryKey.LocalMachine.openSubKey(CLUSTER_REGKEY);
            clusterId = clusterKey.getStringValue(CLUSTER_ID_REGKEY).trim();
            isCluster = true;
        } catch (Win32Exception e) {
            log.debug("Failed to get registry key:" + CLUSTER_REGKEY, e);
        } finally {
            if (clusterKey != null) {
                clusterKey.close();
            }
        }
        return clusterId;
    }

    private static String runCommand(String[] command) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Execute exec = new Execute(new PumpStreamHandler(output));
        exec.setCommandline(command);
        try {
            exec.execute();
        } catch (IOException e) {
            log.warn("Failed to run command: " + exec.getCommandLineString());
            log.warn("File cluster.exe wasn't found.");
            log.debug(e,e);
            return null;
        } catch (Exception e) {
            log.warn("Failed to run command: " + exec.getCommandLineString());
            log.debug(e,e);
            return null;
        }
        String out = output.toString().trim();
        return out;
    }

    private static String getInstanceOwner(String instanceName) {
        // This command lists all MSSQL components and thier active node
        String clusterGroupOutput = runCommand(new String[] { APPCMD, "group" });
        if (clusterGroupOutput == null) {
            return null;
        }

        // Extract components table from the entire command output
        Pattern nodeTablePattern = Pattern.compile("(\\-+)\\s+(\\-+)\\s+(\\-+)(.+)", Pattern.DOTALL);
        Matcher matcher = nodeTablePattern.matcher(clusterGroupOutput);
        if (!matcher.find()) {
            return null;
        }

        String table = matcher.group(4);

        // Find the instance component active node
        Pattern nodeNamePattern = Pattern.compile("[(]" + instanceName + "[)][ ]*([^ ]*)", Pattern.MULTILINE);
        Matcher nameMatcher = nodeNamePattern.matcher(table);

        if (!nameMatcher.find()) {
            return null;
        }

        return nameMatcher.group(1);
    }
}