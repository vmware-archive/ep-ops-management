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

package org.hyperic.util.security;

import java.security.SecureRandom;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;

public class SecurityUtil {
    public final static String ENC_MARK_PREFIX = "ENC(";
    public final static String ENC_MARK_POSTFIX = ")";
    public final static String HIDDEN_VALUE_MASK = "******";
    private static final String VALID_TOKEN_PATTERN = "^[-\\da-fA-F]+$"; // Either a platform token or a UUID (digits,
    // letters and hyphens allowed)
    private static final int VALID_TOKEN_LENGTH = 100;
    private static final String INVALID_TOKEN_LENGTH_ERROR =
                "Invalid token length: ";
    private static final String INVALID_TOKEN_PATTERN_ERROR =
                "Invalid token format";

    /**
     * Generates a token of up to 100 chars of a (generally) random token.
     */
    public static String generateRandomToken() {
        SecureRandom r = new SecureRandom();
        long rand1 = r.nextLong();

        // In case nextLong() draws Long.MIN_VALUE,
        // Math.abs(rand1) will return a negative number
        while (rand1 == Long.MIN_VALUE) {
            rand1 = r.nextLong();
        }

        rand1 = Math.abs(rand1);
        try {
            Thread.sleep(rand1 % 100);
        } catch (InterruptedException e) { // NOPMD
            assert true; // NOP Just to satisfy Checkstyle :-(
        }

        long rand2 = r.nextLong();
        return System.currentTimeMillis() + "-" + Math.abs(rand1) + "-" + Math.abs(rand2);
    }

    public static void validateToken(String token) {
        if (null == token) {
            throw new SecurityException(INVALID_TOKEN_LENGTH_ERROR + "null");
        }
        int tokenLength = token.length();
        if (tokenLength < 0 || tokenLength > VALID_TOKEN_LENGTH) {
            throw new SecurityException(INVALID_TOKEN_LENGTH_ERROR + tokenLength);
        }
        if (!token.matches(VALID_TOKEN_PATTERN)) {
            throw new SecurityException(INVALID_TOKEN_PATTERN_ERROR);
        }
    }

    public static boolean isMarkedEncrypted(String str) {
        if (str == null) {
            return false;
        }
        String uStr = str.toUpperCase();
        return uStr.startsWith(ENC_MARK_PREFIX) && uStr.endsWith(ENC_MARK_POSTFIX);
    }

    public static String unmark(String str) {
        return str.substring(ENC_MARK_PREFIX.length(), str.length() - ENC_MARK_POSTFIX.length());
    }// EOM

    public static String unmarkRecursive(String str) {

        while (str.startsWith(ENC_MARK_PREFIX)) {
            str = str.substring(ENC_MARK_PREFIX.length(), str.length() - ENC_MARK_POSTFIX.length());
        }// EO while there are more parenthesis

        return str;
    }// EOM

    public static String mark(String str) {
        return new StringBuilder().append(ENC_MARK_PREFIX).append(str).append(ENC_MARK_POSTFIX).toString();
    }

    /**
     * 
     * @param encryptor initialized encryptor
     * @param data
     * @return
     */
    public static String encrypt(StringEncryptor encryptor,
                                 String data) {
        return PropertyValueEncryptionUtils.encrypt(data, encryptor);
    }

    public static String encrypt(String encryptionKey,
                                 String data) {

        PBEStringEncryptor encryptor = new SecuredPbeStringEncryptor(encryptionKey);
        return encrypt(encryptor, data);
    }

    public static String decryptRecursiveUnmark(StringEncryptor encryptor,
                                                String data) {
        return encryptor.decrypt(unmarkRecursive(data.trim()));
    }

    public static String decrypt(StringEncryptor encryptor,
                                 String data) {
        return PropertyValueEncryptionUtils.decrypt(data, encryptor);
    }

    public static String decrypt(String encryptionKey,
                                 String data) {
        PBEStringEncryptor encryptor = new SecuredPbeStringEncryptor(encryptionKey);
        return decrypt(encryptor, data);
    }
}
