/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

class CreateOrOverwriteWriter
            extends WriterHandler
{
    private final File destFile;
    private File backupFile;
    private final InputStream inStream;
    private boolean copied;
    private boolean created;

    CreateOrOverwriteWriter(File destFile,
                            InputStream inStream) {
        super();

        this.destFile = destFile;
        this.backupFile = null;
        this.inStream = inStream;
        this.copied = false;
        this.created = false;
    }

    @Override
    public void rollback()
        throws IOException {
        super.rollback();

        if (this.backupFile != null) {
            // If this was an overwrite
            if (this.backupFile.renameTo(destFile) == false) {
                // Try a copy now
                FileInputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(this.backupFile);
                    outputStream = new FileOutputStream(this.destFile);
                    FileUtil.copyStream(inputStream,
                                outputStream);
                } catch (IOException exc) {
                    this.backupFile.delete();
                    throw exc;
                } finally {
                    closeStreams(inputStream, outputStream);
                }
            }
        } else {
            // Else, we created the file. Make sure it's nuked.
            if (this.created == true) {
                this.destFile.delete();
            }
        }
    }

    private void setupBackup()
        throws IOException {
        File parentDir;

        parentDir = this.destFile.getAbsoluteFile().getParentFile();

        if (parentDir == null) {
            throw new IOException("Unable to get the owner directory for " +
                        this.destFile);
        }

        if (this.destFile.exists()) {
            if (!this.destFile.isFile()) {
                throw new IOException(destFile + " is not a regular file");
            }

            // Try within the same directory, first
            try {
                this.backupFile = File.createTempFile("fwrite", "tmp",
                            parentDir);
            } catch (IOException exc) {
                // Else use the system temp directory
                this.backupFile = File.createTempFile("fwrite", "tmp");
            }

            if (this.destFile.renameTo(this.backupFile) == false) {
                // If this fails, attempt a copy
                FileInputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(this.destFile);
                    outputStream = new FileOutputStream(this.backupFile);
                    FileUtil.copyStream(inputStream, outputStream);
                    this.copied = true;
                } catch (IOException exc) {
                    throw new IOException("Unable to rename " + this.destFile +
                                " to " + this.backupFile +
                                " for backup purposes");
                } finally {
                    closeStreams(inputStream, outputStream);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        if (this.backupFile != null) {
            this.backupFile.delete();
            this.backupFile = null;
        }
    }

    @Override
    public void write()
        throws IOException {

        super.write();
        this.setupBackup();
        if (!this.copied) {
            if (this.destFile.createNewFile() == false) {
                throw new IOException("Unable to create " + this.destFile);
            }
            this.created = true;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(this.destFile);
        try {
            IOUtils.copy(this.inStream, fileOutputStream);
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException exc) {
            }
        }
    }

    private static void closeStreams(FileInputStream inputStream,
                                     FileOutputStream outputStream)
        throws IOException {
        try {
            if (null != inputStream) {
                inputStream.close();
            }
        } finally {
            if (null != outputStream) {
                outputStream.close();
            }
        }
    }
}
