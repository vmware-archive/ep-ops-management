/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Transformer<T, R> {
    public Set<R> transformToSet(Collection<T> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<R> rtn = new HashSet<R>();
        transformAndAddToCollection(c, rtn);
        return rtn;
    }

    public List<R> transformToList(Collection<T> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptyList();
        }
        final List<R> rtn = new ArrayList<R>();
        transformAndAddToCollection(c, rtn);
        return rtn;
    }

    public List<R> transform(Collection<T> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptyList();
        }
        final List<R> rtn = new ArrayList<R>(c.size());
        for (T obj : c) {
            R t = transform(obj);
            if (t != null) {
                rtn.add(t);
            }
        }
        return rtn;
    }

    public abstract R transform(T obj);

    private void transformAndAddToCollection(Collection<T> in,
                                             Collection<R> out) {
        for (T obj : in) {
            R t = transform(obj);
            if (t != null) {
                out.add(t);
            }
        }
    }

}
