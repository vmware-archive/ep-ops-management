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

package com.vmware.epops.util.security;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public abstract class HashUtil {

    private final static Logger log = Logger.getLogger(HashUtil.class);
    public static final int HASH_RESULT_LENGTH = 32;

    static public String getStringDigest(String queryInputString) {
        if (StringUtils.isEmpty(queryInputString)) {
            return "EMPTYSTRING";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(queryInputString.getBytes("UTF-8"));
            return new BigInteger(1, md.digest()).toString(16);
        } catch (UnsupportedEncodingException ex) {
            log.error("caught:", ex);
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            log.error("caught:", ex);
            throw new RuntimeException(ex);
        }
    }

}
