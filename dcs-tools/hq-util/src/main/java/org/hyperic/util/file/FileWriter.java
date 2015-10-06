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

package org.hyperic.util.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.MD5;

/**
 * A custom file writer which has the ability to write to files, deal with permissions/ownership, and rollback changes
 * later on.
 */
public class FileWriter extends WriterHandler
{
    private final Log log = LogFactory.getLog(FileWriter.class);

    private File destFile;
    private InputStream inStream;
    private WriterHandler writer;
    private String expectedMD5Sum;

    public FileWriter(File destFile,
                      InputStream inStream,
                      String expectedMD5Sum) {
        super();
        this.destFile = destFile;
        this.inStream = inStream;
        this.writer = new CreateOrOverwriteWriter(this.destFile,
                    this.inStream); // Always using Create or Overwrite mode
        // WISH: Refactor FileWrite mechanism
        this.expectedMD5Sum = expectedMD5Sum;
        ;
    }

    public File getDestFile() {
        return this.destFile;
    }

    @Override
    public void rollback()
        throws IOException {
        super.rollback();

        this.writer.rollback();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        this.writer.cleanup();
    }

    /**
     * Write the file.
     * 
     * @throws IOException if the file write fails.
     */
    @Override
    public void write()
        throws IOException {
        super.write();
        this.writer.write();
    }

    /**
     * Verify the MD5 check sum of the written file.
     * 
     * @throws IOException if the MD5 check sum verification fails.
     * @throws IllegalStateException if the file has not been written yet.
     */
    public void verifyMD5CheckSum()
        throws IOException {
        if (this.writer == null || !this.writer.hasWritten()) {
            throw new IllegalStateException("the file must be written before " +
                        "verifying the MD5 check sum");
        }

        if (this.expectedMD5Sum != null) {
            String actualMD5Sum = MD5.getMD5Checksum(getDestFile());

            log.debug("Verifying MD5 check sum for file " + getDestFile() +
                        "; expected=" + this.expectedMD5Sum + ", actual=" + actualMD5Sum);

            if (!this.expectedMD5Sum.equals(actualMD5Sum)) {
                throw new IOException("MD5 check sum failed for file " +
                            getDestFile() + "; expected=" + this.expectedMD5Sum +
                            ", actual=" + actualMD5Sum);
            }
        }
    }

}
