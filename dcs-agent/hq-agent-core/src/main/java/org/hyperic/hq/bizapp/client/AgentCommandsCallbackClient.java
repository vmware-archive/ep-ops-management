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

package org.hyperic.hq.bizapp.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.FileMetadata;
import org.hyperic.hq.agent.commands.AgentUpdateFiles_result;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.GetDisabledPlugins_args;
import org.hyperic.hq.bizapp.shared.lather.GetDisabledPlugins_result;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.GetAgentCommands_args;
import org.hyperic.hq.bizapp.shared.lather.GetAgentCommands_result;
import org.hyperic.hq.bizapp.shared.lather.SendAgentResponses_args;
import org.hyperic.hq.bizapp.shared.lather.SendAgentResponses_result;
import org.hyperic.hq.common.InvocationRequest;
import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.util.file.FileWriter;
import org.hyperic.util.http.ServerHttpClient;

public class AgentCommandsCallbackClient extends AgentCallbackClient {

    private static final Log LOGGER = LogFactory.getLog(AgentCommandsCallbackClient.class);
    private static final int DOWNLOAD_MAX_SLEEP_TIME = 5 * 60 * 1000;
    private static final int DOWNLOAD_RETRY_COUNT = 10;

    private static enum DOWNLOAD_STATUS {
        DONE, SHOULD_RETRY
    };

    private List<FileWriter> fileWriters = new ArrayList<FileWriter>();

    public AgentCommandsCallbackClient(ProviderFetcher fetcher,
                                       AgentConfig config) {
        super(fetcher, config);
    }

    public List<InvocationRequest> getAgentCommands()
        throws AgentCallbackClientException {
        ProviderInfo provider = this.getProvider();
        GetAgentCommands_result result = (GetAgentCommands_result)
                    this.invokeLatherCall(provider, CommandInfo.CMD_GET_AGENT_COMMANDS, new GetAgentCommands_args());
        return result.getAgentCommands();
    }

    public boolean sendAgentResponses(List<InvocationResponse> responses)
        throws AgentCallbackClientException {
        ProviderInfo provider = this.getProvider();
        SendAgentResponses_args args = new SendAgentResponses_args();
        args.setResponses(responses);
        SendAgentResponses_result result = (SendAgentResponses_result)
                    this.invokeLatherCall(provider, CommandInfo.CMD_SEND_RESPONSE, args);
        return result.isSuccess();
    }

    public List<String> getDisabledPlugins()
        throws AgentCallbackClientException {
        ProviderInfo provider = this.getProvider();
        GetDisabledPlugins_result result =
                    (GetDisabledPlugins_result)
                    this.invokeLatherCall(provider, CommandInfo.CMD_GET_DISABLED_PLUGINS, new GetDisabledPlugins_args());
        return result.getDisabledPlugins();
    }

    /**
     * Downloads files from a list of URLs using the currently configured provider, with retries
     * 
     * @param fileMetaDataList
     * @param result
     * @throws AgentCallbackClientException
     */
    public AgentUpdateFiles_result downloadFilesFromCurrentProvider(List<FileMetadata> fileMetaDataList)
        throws AgentCallbackClientException {
        ProviderInfo providerInfo = null;
        try {
            providerInfo = getProvider();
            return downloadFilesFromProvider(providerInfo, fileMetaDataList);
        } catch (AgentCallbackClientException e) {
            // Should not happen as file Download request is sent only after Agent is set up
            LOGGER.error("Error occurred while attempting to download files: ", e);
            throw e;
        }
    }

    /**
     * Downloads files from a list of URLs (and providerInfo of target server) with retries
     * 
     * @param provider
     * @param fileMetaDataList
     * @param result
     * @throws AgentCallbackClientException
     */
    public AgentUpdateFiles_result downloadFilesFromProvider(ProviderInfo provider,
                                                             List<FileMetadata> fileMetaDataList)
        throws AgentCallbackClientException {

        if (CollectionUtils.isEmpty(fileMetaDataList)) {
            LOGGER.info("No files to download");
            return new AgentUpdateFiles_result();
        }
        if (null == provider) {
            LOGGER.info("No server to download from");
            throw new AgentCallbackClientException("Provider is null");
        }

        LOGGER.info(String.format("%d files are going to be downloaded from the server", fileMetaDataList.size()));

        ServerHttpClient httpClient = getReusableClient();
        int randomNumber = getRandomBaseInterval(20);

        return downloadWithRetries(provider, fileMetaDataList, httpClient, DOWNLOAD_RETRY_COUNT, randomNumber);
    }

    private AgentUpdateFiles_result downloadWithRetries(ProviderInfo provider,
                                                        List<FileMetadata> fileMetaDataList,
                                                        ServerHttpClient httpClient,
                                                        int attemptNum,
                                                        int randomNumber)
        throws AgentCallbackClientException {

        AgentUpdateFiles_result result = new AgentUpdateFiles_result();
        for (FileMetadata fileMetaData : fileMetaDataList) {
            long sleepTimeMillis = 1000 * randomNumber;
            DOWNLOAD_STATUS downloadStatus = DOWNLOAD_STATUS.SHOULD_RETRY;
            for (int i = 0; i < attemptNum; i++) {
                LOGGER.info("Performing download attempt " + String.valueOf(i + 1) + "/" + String.valueOf(attemptNum));

                downloadStatus = downloadSingleFile(httpClient,
                            fileMetaData, provider.getProviderAddress());
                if (downloadStatus == DOWNLOAD_STATUS.DONE) {
                    result.setValue(AgentUpdateFiles_result.FILES_TO_UPDATE
                                + "-" + fileMetaData.getMD5CheckSum(), "true");
                    break;
                }
                // next pass with retries on the remaining files, with increasing interval
                sleepTimeMillis = waitForRetry(sleepTimeMillis);
            }
            if (downloadStatus != DOWNLOAD_STATUS.DONE) {
                LOGGER.error("Could not download all plugins. Rolling back.");
                rollback();
                throw new AgentCallbackClientException("Download attempts exhausted.");
            }
        }

        LOGGER.info(String.format("All %d files in this batch have been downloaded successfully",
                    fileMetaDataList.size()));
        return result;
    }

    private void rollback() {
        for (FileWriter fileWriter : fileWriters) {
            try {
                fileWriter.rollback();
            } catch (IOException exc) {
                LOGGER.error(String.format("Error rolling back '%s': ", fileWriter.getDestFile()), exc);
            }
        }
    }

    /**
     * Downloads a single plugin locally to agent's plugins folder.
     * 
     * @param httpClient The client used to execute the request.
     * @param fileMetadata File's metadata
     * @param providerAddress The provider address.
     * @return DOWNLOAD_STATUS.DONE if there is no further usage for the given plugin URL, meaning no retries will
     *         follow.
     * @throws AgentCallbackClientException
     */

    private DOWNLOAD_STATUS downloadSingleFile(ServerHttpClient httpClient,
                                               FileMetadata fileMetadata,
                                               String providerAddress)
        throws AgentCallbackClientException {
        DOWNLOAD_STATUS downloadStatus;
        String fileName = extractFileNameFromUrl(fileMetadata.getSourceFileUri());

        if (!ProductPluginManager.isValidPluginName(fileName) && !ProductPluginManager.isValidBundleName(fileName)) {
            throw new AgentCallbackClientException("Invalid file name (neither a bundle filename nor a plugin name): "
                        + fileMetadata);
        }
        String absolutePluginUrl = getAbsosultePluginUrl(fileMetadata.getSourceFileUri(), providerAddress);

        LOGGER.info(String.format("Downloading file from: %s", absolutePluginUrl));

        HttpEntity httpEntity = null;

        try {
            HttpResponse response = httpClient.get(absolutePluginUrl, true);
            httpEntity = response.getEntity();

            if (httpEntity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                writeFileToDisk(httpEntity, fileMetadata);
                downloadStatus = DOWNLOAD_STATUS.DONE;
            }
            else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new AgentCallbackClientException(String.format("Could not find file : %s", absolutePluginUrl));
            }
            else {
                LOGGER.error(String.format("Failed downloading file: %s , server returned the following error: %d-%s",
                            absolutePluginUrl, response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase()));
                downloadStatus = DOWNLOAD_STATUS.SHOULD_RETRY;
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Failed downloading file: %s", absolutePluginUrl), e);
            downloadStatus = DOWNLOAD_STATUS.SHOULD_RETRY;
        } finally {
            try {
                EntityUtils.consume(httpEntity);
            } catch (IOException e) {
                LOGGER.error("Failed to consume HttpEntity in order to clean the connection");
            }
        }
        return downloadStatus;
    }

    private boolean writeFileToDisk(HttpEntity httpEntity,
                                    FileMetadata fileMetaData)
        throws IOException,
        AgentCallbackClientException {

        String errorMessage = null;

        InputStream inputStream = httpEntity.getContent();
        LOGGER.info("Preparing to write " + fileMetaData.getDestFileRelativePath());

        FileWriter fileWriter = null;
        try {
            LOGGER.info("Writing to '" + fileMetaData + "'");
            fileWriter = new FileWriter(new File(fileMetaData.getDestFileRelativePath()),
                        inputStream, fileMetaData.getMD5CheckSum());
            fileWriters.add(fileWriter);

            fileWriter.write();
            fileWriter.verifyMD5CheckSum();
            fileWriter.cleanup();
            String destFile = fileWriter.getDestFile().getAbsolutePath();

            LOGGER.info("Successfully wrote: " + destFile);
            return true;

        } catch (IOException exc) {
            errorMessage = "Error writing to '" +
                        fileWriter.getDestFile().getAbsolutePath() + "': " +
                        exc.getMessage();

            String destFile = fileWriter.getDestFile().getAbsolutePath();
            LOGGER.error(errorMessage, exc);
            LOGGER.info("Rolling back '" + destFile + "'");
            try {
                fileWriter.rollback();
            } catch (IOException e) {
                LOGGER.error("Error rolling back '" + destFile +
                            ": " + e.getMessage(), e);
            }
            throw new AgentCallbackClientException(exc);
        } finally {
            inputStream.close();
        }
    }

    private String getAbsosultePluginUrl(String relativePluginUrl,
                                         String providerAddress) {
        String absolutePluginUrl = providerAddress + relativePluginUrl;
        absolutePluginUrl = absolutePluginUrl.replaceFirst("\\/epops-webapp\\/lather", "");
        return absolutePluginUrl;
    }

    private String extractFileNameFromUrl(String relativePluginUrl) {
        String pluginName = relativePluginUrl.substring(relativePluginUrl.lastIndexOf('/') + 1);
        return pluginName;
    }

    private long waitForRetry(long sleepTimeMillis) {
        LOGGER.error("Could not download file, waiting for " + String.valueOf(sleepTimeMillis / 1000)
                    + " seconds before retrying.");
        try {
            Thread.sleep(sleepTimeMillis);
            sleepTimeMillis = sleepTimeMillis * 2;
            if (sleepTimeMillis > DOWNLOAD_MAX_SLEEP_TIME) {
                sleepTimeMillis = DOWNLOAD_MAX_SLEEP_TIME;
            }
        } catch (InterruptedException e) {
        }
        return sleepTimeMillis;
    }

    private int getRandomBaseInterval(int maxValue) {
        Random rand = new Random();
        int randomNumber = rand.nextInt(maxValue) + 1;
        return randomNumber;
    }
}
