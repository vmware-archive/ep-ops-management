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

package org.hyperic.hq.bizapp.shared.lather;

import java.io.Serializable;
import java.util.List;

import org.hyperic.hq.common.InvocationResponse;
import org.hyperic.lather.LatherValue;

public class SendAgentResponses_args extends LatherValue {

    private static String RESPONSES = "responses";

    @SuppressWarnings("unchecked")
    public List<InvocationResponse> getResponses() {
        return (List<InvocationResponse>) getObject(RESPONSES);
    }

    public void setResponses(List<InvocationResponse> responses) {
        addObject(RESPONSES, (Serializable) responses);
    }
}
