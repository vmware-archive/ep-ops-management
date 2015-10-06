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

package org.hyperic.util.pager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A utility class that contains all a "page" of data that is viewable <br>
 * this list may or may not contain the entire list of information. generally this list contains a subset of data. <br>
 * ex. say we have a list of 5000 users. the entire list does not need to be returned to only display the first 15
 * items, the user is only going to see the first 15 both the user and the application the user will want to know that
 * there are 5000 users in the system. <br>
 * 
 */
public class PageList<T> extends ArrayList<T> implements Serializable {
    private int totalSize = 0;
    private boolean isUnbounded; // Is the total size of the list known?
    private Serializable metaData;

    public PageList() {
        super();
        this.isUnbounded = false;
    }

    public PageList(Collection<T> c,
                    int totalSize) {
        super(c);
        this.totalSize = totalSize;
        this.isUnbounded = false;
    }

    public String toString() {
        StringBuffer s = new StringBuffer("{")
                    .append("totalSize=")
                    .append(totalSize).append(" ")
                    .append("}");
        return super.toString() + s.toString();

    }

    /**
     * returns the total size of the "masterlist" that this page is a subset of.
     * 
     * @return Value of property listSize.
     */
    public int getTotalSize() {
        return Math.max(this.size(), this.totalSize);
    }

    /**
     * Sets the total size of the "masterlist" that this page is a subset of.
     * 
     * @param totalSize New value of property totalSize.
     * 
     */
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public void setMetaData(Serializable metaData) {
        this.metaData = metaData;
    }

    public Serializable getMetaData() {
        return this.metaData;
    }

    public boolean isUnbounded() {
        return this.isUnbounded;
    }

    public void setUnbounded(boolean isUnbounded) {
        this.isUnbounded = isUnbounded;
    }
}
