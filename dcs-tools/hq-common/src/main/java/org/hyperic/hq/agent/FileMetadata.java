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

package org.hyperic.hq.agent;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.commons.lang.StringUtils;

public class FileMetadata implements Externalizable {
    private String sourceFileUri;
    private String destFileRelativePath;
    private String md5sum;

    /**
     * Default constructor for externalization only. When an Externalizable object is reconstructed, an instance is
     * created using this public no-arg constructor, then the readExternal method called.
     */
    public FileMetadata() {
    }

    public FileMetadata(String sourceFileURI,
                        String destFileRelativePath,
                        String md5sum) {
        if (StringUtils.isBlank(sourceFileURI) || StringUtils.isBlank(destFileRelativePath)
                    || StringUtils.isBlank(md5sum)) {
            throw new NullPointerException("Source or destination location is empty, whitespace or null");
        }

        this.sourceFileUri = sourceFileURI;
        this.destFileRelativePath = destFileRelativePath;
        this.md5sum = md5sum;
    }

    public String getSourceFileUri() {
        return sourceFileUri;
    }

    public String getDestFileRelativePath() {
        return destFileRelativePath;
    }

    public String getMD5CheckSum() {
        return md5sum;
    }

    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        this.sourceFileUri = in.readUTF();
        this.destFileRelativePath = in.readUTF();
        this.md5sum = in.readUTF();
    }

    public void writeExternal(ObjectOutput out)
        throws IOException {
        out.writeUTF(sourceFileUri);
        out.writeUTF(this.destFileRelativePath);
        out.writeUTF(this.md5sum);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("File metadata of:\n");
        sb.append(sourceFileUri);
        sb.append("\n\twith md5: ");
        sb.append(md5sum);
        sb.append("\n\t dest. path: ");
        sb.append(destFileRelativePath);

        return sb.toString();
    }
}
