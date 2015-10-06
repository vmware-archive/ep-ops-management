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

/**
 * Provides a point of extensibility in the paging behavior. If you supply a PagerProcessor when you get a Pager, then
 * that processor will be called to process each element as the pager moves it from the source collection to the
 * destination collection.
 */
public interface PagerProcessorExt extends PagerProcessor {

    /**
     * Get the event handler for this pager. May return null to indicate that no event handler should be used.
     */
    public PagerEventHandler getEventHandler();

    /**
     * Determines if null values are included in the Pager's results.
     * 
     * @return If this method returns true, then when the processElement method returns null, that element will not be
     *         included in the results. If this methods returns false, then nulls may be added to the result page.
     */
    public boolean skipNulls();

    /**
     * Process an element as the pager moves it from the source collection to the destination collection. This version
     * allows an additional argument to be passed along.
     * 
     * @param o1 The object to process.
     * @param o2 Additional data required to processElement.
     * @return The processed object.
     */
    public Object processElement(Object o1,
                                 Object o2);

}
