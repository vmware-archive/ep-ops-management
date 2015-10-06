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

import org.hyperic.lather.LatherValue;

public class SendAgentResponses_result extends LatherValue {

    public static String COMMAND_RESULT = "success";
    public static String ERROR_MESSAGE = "errorMessage";

    public boolean isSuccess() {
        if (null != getObject(COMMAND_RESULT)) {
            return (Boolean) getObject(COMMAND_RESULT);
        }
        return false;
    }

    public void setSuccess(boolean result) {
        addObject(COMMAND_RESULT, Boolean.valueOf(result));
    }

    public void setErrorMessage(String errorMessage) {
        addObject(ERROR_MESSAGE, errorMessage);
    }

    public String getErrorMessage() {
        if (null != getObject(ERROR_MESSAGE)) {
            return (String) getObject(ERROR_MESSAGE);
        }
        return null;
    }

    @Override
    public String toString() {
        return "SendAgentResponses_result [isSuccess()=" + isSuccess()
                    + ", getErrorMessage()=" + getErrorMessage() + ", toString()="
                    + super.toString() + "]";
    }

}
