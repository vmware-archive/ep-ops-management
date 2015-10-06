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

package com.vmware.epops.command.downstream.mail.management;

import java.util.Arrays;

import com.vmware.epops.command.downstream.mail.AgentMailCommandType;

public class AgentUpdateFilesCommand extends AgentManagementCommand {

    public static final AgentMailCommandType COMMAND_TYPE = AgentMailCommandType.AGENT_UPDATE_FILES;

    private FileMetadata[] filesToUpdate = {};
    private String[] filesToRemove = {};
    private boolean restartIfSuccessful;

    @Override
    public AgentMailCommandType getCommandType() {
        return COMMAND_TYPE;
    }

    public FileMetadata[] getFilesToUpdate() {
        return Arrays.copyOf(filesToUpdate, filesToUpdate.length);
    }

    public String[] getFilesToRemove() {
        return Arrays.copyOf(filesToRemove, filesToRemove.length);
    }

    public boolean isRestartIfSuccessful() {
        return restartIfSuccessful;
    }

    public void setRestartIfSuccessful(boolean restartIfSuccessful) {
        this.restartIfSuccessful = restartIfSuccessful;
    }

    public void setFilesToUpdate(FileMetadata[] filesToUpdate) {
        this.filesToUpdate = Arrays.copyOf(filesToUpdate, filesToUpdate.length);
    }

    public void setFilesToRemove(String[] filesToRemove) {
        this.filesToRemove = Arrays.copyOf(filesToRemove, filesToRemove.length);
    }

    public static class FileMetadata {
        private String sourceFileURI;
        private String destFileRelativePath;
        private String md5sum;

        public void setSourceFileURI(String sourceFileURI) {
            this.sourceFileURI = sourceFileURI;
        }

        public void setDestFileRelativePath(String destFileRelativePath) {
            this.destFileRelativePath = destFileRelativePath;
        }

        public void setMd5sum(String md5sum) {
            this.md5sum = md5sum;
        }

        public String getSourceFileURI() {
            return sourceFileURI;
        }

        public String getDestFileRelativePath() {
            return destFileRelativePath;
        }

        public String getMd5sum() {
            return md5sum;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("File metadata of:\n");
            sb.append(sourceFileURI);
            sb.append("\n\twith md5: ");
            sb.append(md5sum);
            sb.append("\n\t dest. path: ");
            sb.append(destFileRelativePath);

            return sb.toString();
        }
    }
}
