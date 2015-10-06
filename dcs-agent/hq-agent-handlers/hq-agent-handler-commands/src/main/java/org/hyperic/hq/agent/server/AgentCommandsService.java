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

package org.hyperic.hq.agent.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.FileMetadata;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentUpdateFiles_result;
import org.hyperic.hq.agent.commands.AgentUpgrade_result;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.AgentCommandsCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.security.MD5;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Agent commands business logic.These are the methods that are invoked directly by the command from the mailbox. Beware
 * of changing the API, without changing the server side as well
 */
public class AgentCommandsService implements AgentCommandsClient {

    private static final String AGENT_BUNDLE_HOME = AgentConfig.AGENT_BUNDLE_HOME;

    private static final Log LOGGER = LogFactory.getLog(AgentCommandsService.class);

    private final AgentDaemon agentDaemon;

    private final AgentConfig agentConfig;

    private AgentCommandsCallbackClient agentCommandsCallbackClient = null;

    private static enum PLUGINS_STATUS {
        NO_PLUGINS_INCLUDED, PLUGINS_DIFFER_FROM_SERVER, PLUGINS_ALREADY_IN_SYNC
    };

    public AgentCommandsService(AgentDaemon agent)
        throws AgentRunningException {
        agentDaemon = agent;
        agentConfig = agentDaemon.getBootConfig();
        agentCommandsCallbackClient =
                    new AgentCommandsCallbackClient(new StorageProviderFetcher(agentDaemon.getStorageProvider()),
                                agentDaemon.getBootConfig()); // Using a new invocation ,rather than fetching it from
        // AgentTransport as it doesn't matter and is harder the other way
    }

    public AgentUpdateFiles_result agentUpdateFiles(FileMetadata[] filesToUpdate,
                                                    String[] filesToRemove,
                                                    Boolean restartIfSuccessful)
        throws AgentRemoteException, AgentConnectionException {
        return agentUpdateFiles(filesToUpdate, filesToRemove, restartIfSuccessful.booleanValue());
    }

    public AgentUpdateFiles_result agentUpdateFiles(FileMetadata[] filesToUpdate,
                                                    String[] filesToRemove,
                                                    boolean restartIfSuccessful)
        throws AgentRemoteException, AgentConnectionException {

        boolean filesToUpdateRequestExists = !ArrayUtils.isEmpty(filesToUpdate);
        boolean filesToRemoveRequestExists = !ArrayUtils.isEmpty(filesToRemove);
        PLUGINS_STATUS pluginsStatus = PLUGINS_STATUS.NO_PLUGINS_INCLUDED;

        AgentUpdateFiles_result result = new AgentUpdateFiles_result();

        if (!filesToUpdateRequestExists && !filesToRemoveRequestExists) {
            LOGGER.info("An empty update files request has been recieved. Ignoring.");
            return result;
        }
        LOGGER.info("Update files request has been recieved");

        if (filesToRemoveRequestExists) {
            try {
                pluginsStatus = markFilesToRemoveIfNeeded(filesToRemove, result, pluginsStatus);
            } catch (Exception e) {
                // no point to continue
                result.setValue(AgentUpdateFiles_result.FILES_TO_REMOVE, "failed");
                LOGGER.error("Quitting as removing files has failed.", e);
                return result;
            }
        } else {
            LOGGER.info("No files to remove.");
        }

        if (filesToUpdateRequestExists) {
            try {
                pluginsStatus = updateFilesIfNeeded(filesToUpdate, result, pluginsStatus);
            } catch (Exception e) {
                // no point to continue
                result.setValue(AgentUpdateFiles_result.FILES_TO_UPDATE, "failed");
                LOGGER.error("Quitting as updating files has failed.", e);
                return result;
            }
        } else {
            LOGGER.info("No files to update.");
        }

        if (restartIfSuccessful) {
            LOGGER.info("Update requires agent restart, restarting agent...");
            restart();
        } else if (pluginsStatus == PLUGINS_STATUS.PLUGINS_DIFFER_FROM_SERVER) {
            LOGGER.info("Update requires restarting the plugin managers, restarting them ...");
            applyPluginsChanges(); // this is relevant only if we have changes in the plugins.
        } else if (pluginsStatus == PLUGINS_STATUS.PLUGINS_ALREADY_IN_SYNC) {
            LOGGER.info("Plugins from this request already exist on disk. Informing the server.");
            agentDaemon.sendPluginStatusToServer(false);
        }
        return result;
    }

    private FileMetadata[] filterFilesToUpdate(FileMetadata[] files)
        throws AgentCallbackClientException {
        List<String> disabledPlugins = agentCommandsCallbackClient.getDisabledPlugins();
        List<FileMetadata> filteredPlugins = new ArrayList<FileMetadata>();
        for (FileMetadata fileMetaData : files) {
            boolean isDisabled = false;
            for (String disabledPlugin : disabledPlugins) {
                if (fileMetaData.getDestFileRelativePath().contains(disabledPlugin)) {
                    isDisabled = true;
                    break;
                }
            }
            if (!isDisabled) {
                filteredPlugins.add(fileMetaData);
            }
        }
        FileMetadata[] filteredArray = new FileMetadata[filteredPlugins.size()];
        filteredPlugins.toArray(filteredArray);
        return filteredArray;
    }

    private PLUGINS_STATUS updateFilesIfNeeded(FileMetadata[] filesToUpdate,
                                               AgentUpdateFiles_result result,
                                               PLUGINS_STATUS pluginsStatus)
        throws AgentRemoteException, AgentCallbackClientException {

        if (null == filesToUpdate || null == result) {
            return pluginsStatus;
        }

        FileMetadata[] filteredFiles = filterFilesToUpdate(filesToUpdate);
        if (filteredFiles.length == 0) {
            return PLUGINS_STATUS.PLUGINS_ALREADY_IN_SYNC;
        }

        List<FileMetadata> fileMetaDataList = new ArrayList<FileMetadata>(filesToUpdate.length);
        for (FileMetadata fileMetaData : filteredFiles) {
            String sourceFileUri = fileMetaData.getSourceFileUri();
            String md5CheckSum = fileMetaData.getMD5CheckSum();
            String destFileRelativePath = fileMetaData.getDestFileRelativePath();
            if (StringUtil.isNullOrEmpty(destFileRelativePath)
                        || StringUtil.isNullOrEmpty(sourceFileUri)
                        || StringUtil.isNullOrEmpty(md5CheckSum)) {
                // in case the server sends plugins but all plugin files were missing a part in the command,
                // we won't send a plugins report back to the server.
                // we assume this command is damaged.
                // this may lead to an agent staing in the "Unsynchronized Agents" group till its next sync.
                // the alternative is a possible endless round of sending and responding which is worst.
                // (same as when download fails - bug 1462366 )
                continue;
            }
            String resolvedFilePath = resolveAgentBundleHomePath(destFileRelativePath);
            boolean isPlugin = isPluginFile(destFileRelativePath);
            if (isPlugin && pluginsStatus == PLUGINS_STATUS.NO_PLUGINS_INCLUDED) { // if it is not marked yet as plugin
                pluginsStatus = PLUGINS_STATUS.PLUGINS_ALREADY_IN_SYNC;
            }
            // filter existing files, by MD5 hash
            if (shouldDownloadFileByMd5(resolvedFilePath, md5CheckSum)) {
                if (isPlugin) {
                    pluginsStatus = PLUGINS_STATUS.PLUGINS_DIFFER_FROM_SERVER;
                }
                resolvedFilePath = convertPdkDirToTmpDirIn(injectUpdatePluginSuffix(resolvedFilePath));
                LOGGER.info(String.format("About to download file: %s, checksum = %s, into: %s",
                            sourceFileUri, md5CheckSum, resolvedFilePath));
                FileMetadata resolvedFileMetaData = new FileMetadata(
                            fileMetaData.getSourceFileUri(), resolvedFilePath, fileMetaData.getMD5CheckSum());
                fileMetaDataList.add(resolvedFileMetaData);
            } else {
                LOGGER.info(String.format("Skipping file download from: %s, file already exists locally.",
                            sourceFileUri));
                result.setValue(AgentUpdateFiles_result.FILES_TO_UPDATE + "-" + fileMetaData.getMD5CheckSum(), "true");
            }
        }

        result.merge(agentCommandsCallbackClient.downloadFilesFromCurrentProvider(fileMetaDataList));
        return pluginsStatus;
    }

    private PLUGINS_STATUS markFilesToRemoveIfNeeded(String[] filesToRemove,
                                                     AgentUpdateFiles_result result,
                                                     PLUGINS_STATUS pluginsStatus)
        throws Exception {

        if (null == filesToRemove || null == result) {
            return pluginsStatus;
        }

        // Prepare files to be removed from disk on next restart
        LinkedList<String> filesToRemoveWithCorrectedPath = new LinkedList<String>();

        for (String fileToRemove : filesToRemove) {
            if (StringUtil.isNullOrEmpty(fileToRemove)) {
                continue;
            }
            String resolvedFilePath = resolveAgentBundleHomePath(fileToRemove);
            boolean isPlugin = isPluginFile(resolvedFilePath);
            if (isPlugin && pluginsStatus == PLUGINS_STATUS.NO_PLUGINS_INCLUDED) { // if it is not marked yet as plugin
                pluginsStatus = PLUGINS_STATUS.PLUGINS_ALREADY_IN_SYNC;
            }
            if (!(new File(resolvedFilePath).exists())) {
                LOGGER.info(String.format("Skipping remove request for %s, as it doesn't exist on local disk",
                            resolvedFilePath));
                result.setValue(AgentUpdateFiles_result.FILES_TO_REMOVE + "-" + fileToRemove, "true");
                continue;
            }
            if (isPlugin) {
                pluginsStatus = PLUGINS_STATUS.PLUGINS_DIFFER_FROM_SERVER;
            }
            filesToRemoveWithCorrectedPath.add(convertPdkDirToTmpDirIn(injectRemovePluginSuffix(resolvedFilePath)));
        }
        Map<String, Boolean> filesRegisteredToRemove = agentRemoveFiles(filesToRemoveWithCorrectedPath);
        LOGGER.info(String.format("%d files have been set to be removed",
                    filesToRemoveWithCorrectedPath.size()));

        for (String key : filesRegisteredToRemove.keySet()) {
            result.setValue(AgentUpdateFiles_result.FILES_TO_REMOVE + "-" + key,
                        filesRegisteredToRemove.get(key).toString());
        }
        return pluginsStatus;
    }

    /**
     * checks if the given file name match the plugins file name convention.
     * 
     * @param fileName
     * @return true if given file name match the plugins file name convention
     */
    private boolean isPluginFile(String fileName) {
        return ProductPluginManager.isValidPluginName(fileName);
    }

    /**
     * checks if the current Java version match for calling applyPluginsChanges
     */
    private boolean isValidJavaVersionToApplyPluginsChanges() {
        return ProductPluginManager.isValidJavaVersionToApplyPluginsChanges();
    }

    private void applyPluginsChanges()
        throws AgentConnectionException, AgentRemoteException {
        if (!isValidJavaVersionToApplyPluginsChanges()) {
            LOGGER.debug("Invalid java version for calling applyPluginsChanges...");
            LOGGER.info("Update requires agent restart, restarting agent...");
            restart();
        }
        try {
            agentDaemon.applyPluginsChanges();
        } catch (IOException ioe) {
            throw new AgentConnectionException("IO Exception", ioe);
        } catch (AgentStartException ase) {
            throw new AgentConnectionException("Agent Start Exception", ase);
        } catch (PluginException pe) {
            throw new AgentRemoteException("Plugin Exception", pe);
        }
    }

    private boolean shouldDownloadFileByMd5(String destFileRelativePath,
                                            String expectedMd5) {
        File destFile = new File(destFileRelativePath);
        if (!destFile.exists()) {
            return true;
        }
        String existingMd5 = MD5.getMD5Checksum(destFile);
        if (!existingMd5.equals(expectedMd5)) {
            return true;
        }
        return false;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#die()
     */
    public void die()
        throws AgentRemoteException {
        try {
            agentDaemon.die();
        } catch (AgentRunningException exc) {
            // This should really never happen
            LOGGER.error("Killing a running agent!");
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping()
        throws AgentRemoteException {
        return 0;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#restart()
     */
    public void restart()
        throws AgentRemoteException {
        AgentUpgradeManager.restartJVM();
    }

    private void restartKeepNextSyncScanTime()
        throws AgentRemoteException {
        try {
            agentDaemon.getSyncModeManager().storeNextSyncScanTime();
        } catch (Exception ex) {
            LOGGER.error("failed to storeNextSyncScanTime.", ex);
        }
        agentDaemon.restartGracefully();
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#getCurrentAgentBundle()
     */
    public String getCurrentAgentBundle()
        throws AgentRemoteException {
        return agentDaemon.getCurrentAgentBundle();
    }

    private void setExecuteBit(File file)
        throws AgentRemoteException {
        int timeout = 10 * 6000;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecuteWatchdog watch = new ExecuteWatchdog(timeout);
        Execute exec = new Execute(new PumpStreamHandler(output), watch);
        int rc;

        try {
            String[] arguments = { "chmod", "+x", file.getCanonicalPath() };
            exec.setCommandline(arguments);

            LOGGER.info("Running " + exec.getCommandLineString());
            rc = exec.execute();
        } catch (Exception e) {
            rc = -1;
            LOGGER.error(e);
        }

        if (rc != 0) {
            String msg = output.toString().trim();
            if (msg.length() == 0) {
                msg = "timeout after " + timeout + "ms";
            }
            throw new AgentRemoteException("Failed to set permissions: " + "[" +
                        exec.getCommandLineString() + "] " +
                        msg);
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#upgrade(java.lang.String, java.lang.String)
     */
    public Map upgrade(String bundle,
                       String destination)
        throws AgentRemoteException {
        bundle = resolveAgentBundleHomePath(bundle);
        destination = resolveAgentBundleHomePath(destination);
        final File bundleFile = new File(bundle);
        final File workDir = new File(destination, "work");

        Map result = new HashMap();

        try {

            LOGGER.info("Preparing to upgrade agent bundle from file " + bundle +
                        " at destination " + destination);
            // check that we are running in Java Service Wrapper mode
            if (!WrapperManager.isControlledByNativeWrapper()) {
                throw new AgentRemoteException(
                            "Upgrade command is not supported without the Java Service Wrapper.");
            }

            // check that the bundle file exists and is a file
            // we are assuming at this point that the file is not corrupted
            if (!bundleFile.isFile()) {
                throw new AgentRemoteException("Upgrade agent bundle "
                            + bundle + " is not a valid file");
            }

            // assume that the bundle name is the same as the top level directory
            String bundleHome = getBundleHome(bundleFile);

            // delete work directory in case it wasn't cleaned up
            FileUtil.deleteDir(workDir);
            // extract to work directory
            try {
                FileUtil.decompress(bundleFile, workDir);
            } catch (IOException e) {
                LOGGER.error("Failed to decompress " + bundle + " at destination " + workDir, e);
                throw new AgentRemoteException(
                            "Failed to decompress " + bundle + " at destination " + workDir);
            }

            // check if the bundle home directory exists
            File bundleDir = new File(destination, bundleHome);

            final File extractedBundleDir = new File(workDir, bundleHome);
            // verify that top level dir exists
            if (!extractedBundleDir.isDirectory()) {
                throw new AgentRemoteException(
                            "Invalid agent bundle file detected; missing top-level "
                                        + bundleDir + " directory");
            }

            if (bundleDir.exists()) {
                // TODO HQ-2428 Since we use maven and no longer have build numbers, there needs to be a way to
                // differentiate between snapshot builds. After some discussion,
                // we decided to ensure bundle folder name uniqueness by timestamp. This means that users could
                // "upgrade" to the same version, HQ will no longer prevent
                // this scenario...
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

                bundleHome += "-" + dateFormat.format(new Date(System.currentTimeMillis()));
                bundleDir = new File(destination, bundleHome);
            }

            // if everything went well, move extracted files to destination
            if (!extractedBundleDir.renameTo(bundleDir)) {
                throw new AgentRemoteException(
                            "Failed to copy agent bundle from " + extractedBundleDir + " to " + bundleDir);
            }

            // Handle potential permissions issues
            if (!JDK.IS_WIN32) {
                File pdkdir = new File(bundleDir, "pdk");
                File pdklibdir = new File(pdkdir, "lib");
                File jredir = new File(pdkdir, "jre");
                if (!pdklibdir.exists()) {
                    throw new AgentRemoteException("Invalid PDK library directory " +
                                pdklibdir.getAbsolutePath());
                }

                File[] libs = pdklibdir.listFiles();
                for (File lib : libs) {
                    if (lib.getName().endsWith("sl")) {
                        // chmod +x ./bundles/$AGENT_BUNDLE/pdk/lib/*.sl
                        setExecuteBit(lib);
                    }
                }

                if (jredir.exists()) {
                    File jrebin = new File(jredir, "bin");
                    File[] bins = jrebin.listFiles();
                    // chmod +x ./bundles/$AGENT_BUNDLE/jre/bin/*
                    for (File bin : bins) {
                        setExecuteBit(bin);
                    }
                }

                File pdkscriptsdir = new File(pdkdir, "scripts");
                if (!pdkscriptsdir.exists()) {
                    throw new AgentRemoteException("Invalid PDK scripts directory " +
                                pdklibdir.getAbsolutePath());
                }

                File[] scripts = pdkscriptsdir.listFiles();
                for (File script : scripts) {
                    // chmod +x ./bundles/$AGENT_BUNDLE/pdk/scripts/*
                    setExecuteBit(script);
                }
            }

            // update the wrapper configuration for next JVM restart
            boolean success = false;
            try {
                success = AgentUpgradeManager.upgrade(bundleHome);
            } catch (IOException e) {
                LOGGER.error("Failed to write new bundle home " + bundleHome
                            + " into rollback properties", e);
            } finally {
                if (!success) {
                    throw new AgentRemoteException(
                                "Failed to write new bundle home " + bundleHome
                                            + " into rollback properties");
                }
            }
            LOGGER.info("Successfully upgraded to new agent bundle");
            FileInputStream versionInputStream = null;
            try {
                LOGGER.debug("Creating result map");

                // Grab the hq-version.properties file from the new hq bundle
                final File versionFile = new File(bundleDir, "lib/hq-version.properties");
                versionInputStream = new FileInputStream(versionFile);

                Properties newVersionProperties = new Properties();
                newVersionProperties.load(versionInputStream);

                // Created return map
                String version = newVersionProperties.getProperty("version");

                LOGGER.debug("VERSION: " + version);
                LOGGER.debug("BUNDLE_NAME: " + bundleHome);

                result.put(AgentUpgrade_result.VERSION, version);
                result.put(AgentUpgrade_result.BUNDLE_NAME, bundleHome);
            } catch (MalformedURLException e) {
                LOGGER.warn("Could not access new hq-version.properties due to a malformed url, version value will not be updated in the database.",
                            e);
            } catch (IOException e) {
                LOGGER.warn("Could not read new hq-version.properties file, version value will not be updated in the database.",
                            e);
            } finally {
                if (versionInputStream != null) {
                    try {
                        versionInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        // cleanup work dir files and bundle
        finally {
            doUpgradeCleanup(bundleFile, workDir);
        }

        return result;
    }

    private void doUpgradeCleanup(File bundleFile,
                                  File workDir) {
        bundleFile.delete();
        // recursive delete
        FileUtil.deleteDir(workDir);
    }

    private String getBundleHome(File bundleFile)
        throws AgentRemoteException {
        final int index;
        String fileName = bundleFile.getName();
        if (fileName.endsWith(".tar.gz")) {
            index = fileName.lastIndexOf(".tar.gz");
        }
        else if (fileName.endsWith(".tgz")) {
            index = fileName.lastIndexOf(".tgz");
        }
        else if (fileName.endsWith(".zip")) {
            index = fileName.lastIndexOf(".zip");
        }
        else {
            throw new AgentRemoteException(
                        "Invalid file format for the agent bundle tar file (.zip, .tar.gz or .tgz expected)");
        }
        return fileName.substring(0, index);
    }

    /**
     * Prepares files to be removed after AgentDaemon is restarted. Creates the files in their destination directory.
     * This method assumes its user supplies a correct path (which takes into account knowledge of agent inner workings)
     * 
     * Upon restart AgentDaemon checks the /tmp folder and if it contains plugins ending with -remove.jar/-remove.xml,
     * it will delete them. This impl. has been chosen to avoid file locks preventing the files to be deleted
     * immediately
     * 
     * @throws Exception
     */
    private Map<String, Boolean> agentRemoveFiles(Collection<String> files)
        throws Exception {
        final Map<String, Boolean> rtn = new HashMap<String, Boolean>();
        final boolean debug = LOGGER.isDebugEnabled();
        for (final String filename : files) {
            if (filename == null) {
                continue;
            }
            try {
                final File file = new File(resolveAgentBundleHomePath(filename));
                file.createNewFile();
                if (debug) {
                    LOGGER.debug("Preparing file for removal:" + file.getAbsolutePath());
                }
                rtn.put(filename, file.exists());
            } catch (Exception e) {
                LOGGER.error("Quitting. Could not prepare file for removal - " + filename + ": " + e);
                throw e;
            }
        }
        return rtn;
    }

    /**
     * Replaces tokenized agent.bundle.home property from path
     * 
     * @param path
     * @return
     * @throws AgentRemoteException
     */
    private String resolveAgentBundleHomePath(String path)
        throws AgentRemoteException {
        String agentBundleHome = System.getProperty(AGENT_BUNDLE_HOME);
        // this should never happen
        if (agentBundleHome == null) {
            throw new AgentRemoteException(
                        "Could not resolve system property " + AGENT_BUNDLE_HOME);
        }
        String resolvedFilePath = StringUtil.replace(path, "${" + AGENT_BUNDLE_HOME + "}", agentBundleHome);
        if (!path.equals(resolvedFilePath)) {
            LOGGER.debug(String.format("Converted %s to %s", path, resolvedFilePath));
        }
        return resolvedFilePath;
    }

    /**
     * Replaces PDK directory in path with agent/bundles/tmp directory for agent's specific plugin management
     * functionality. In HQ, the server would send this tmp path by itself. Here we would like to hide the functionality
     * from the server and make it an agent's responsibility
     * 
     * @param path
     * @return
     * @throws AgentRemoteException
     */
    private String convertPdkDirToTmpDirIn(String path)
        throws AgentRemoteException {

        Properties bootProps = agentConfig.getBootProperties();
        String tmpDir = bootProps.getProperty(AgentConfig.PROP_TMPDIR[0]);
        String pluginsDir = bootProps.getProperty(AgentConfig.PROP_PDK_PLUGIN_DIR[0]);
        if (StringUtil.isNullOrEmpty(tmpDir) || StringUtil.isNullOrEmpty(pluginsDir)) {
            throw new AgentRemoteException("Could not resolve one of the following system properties, or both:"
                        + AgentConfig.PROP_TMPDIR[0] + ","
                        + AgentConfig.PROP_PDK_PLUGIN_DIR[0]);
        }
        String resolvedFilePath = StringUtil.replace(path, pluginsDir, tmpDir);
        if (!path.equals(resolvedFilePath)) {
            LOGGER.debug(String.format("Converted %s to %s", path, resolvedFilePath));
        }
        return resolvedFilePath;
    }

    /**
     * Injecting the suffix that denotes to AgentDaemon to remove the files on restart
     * 
     * @param path
     * @return
     */
    private String injectRemovePluginSuffix(String path) {
        // See also AgentManagerImpl.java:agentRemovePlugins(...) in HQ Server
        return injectSuffixIfPluginFilePath(path, AgentUpgradeManager.REMOVED_PLUGIN_EXTENSION);
    }

    /**
     * Injecting the suffix that denotes to AgentDaemon to add/update the files on restart
     * 
     * @param path
     * @return
     */
    private String injectUpdatePluginSuffix(String path) {
        // See also AgentManagerImpl.java:transferAgentPlugins(...) in HQ Server
        return injectSuffixIfPluginFilePath(path, AgentUpgradeManager.UPDATED_PLUGIN_EXTENSION);
    }

    /**
     * Injects suffix right before file extension, only if this is a valid plugin name (To avoid injecting these files,
     * in case this is a bundle upgrade, for instance)
     * 
     * @param path
     * @param suffix
     * @return
     */
    private String injectSuffixIfPluginFilePath(String path,
                                                String suffix) {
        if (isPluginFile(path)) {
            return new StringBuilder(path).insert(path.length() - 4, suffix).toString();
        } else {
            return path;
        }
    }
}
