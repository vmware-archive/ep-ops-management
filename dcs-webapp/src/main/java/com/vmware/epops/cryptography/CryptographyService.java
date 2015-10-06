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

package com.vmware.epops.cryptography;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class CryptographyService {

    @SuppressWarnings("PMD")
    private static String keyLocation;

    private final static Logger logger = Logger.getLogger(CryptographyService.class);

    private static long keyTimeStamp = -1;
    private static String key;
    private static Cipher cipher = null;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value =
                "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "We inject with spring and use"
                            + " the class from static methods(from non-spring code)")
    @Autowired
    public CryptographyService(@Value("${epops.keystore.location}") String keyLocation) {
        logger.info("Initializing CryptographyService for '" + keyLocation + "'");
        CryptographyService.keyLocation = keyLocation;
    }

    private static void updateCipher()
        throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
        InvalidAlgorithmParameterException {
        logger.info("Update cipher from '" + keyLocation + "'");
        key = getKey(keyLocation);
        keyTimeStamp = new File(keyLocation).lastModified();

        byte[] keyBytes = key.getBytes("UTF-8");
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(keyBytes);

        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings({ "DM_DEFAULT_ENCODING", "REC_CATCH_EXCEPTION" })
    public synchronized static String decrypt(String text) {
        String realText = null;

        try {
            if (text == null) {
                return null;
            }

            if (isKeyChanged()) {
                updateCipher();

            }

            text = text.trim();
            if (text.endsWith("@")) {
                text = text.substring(0, text.length() - 1);
            }
            text = text.trim();

            byte[] textBytes = Base64.decodeBase64(text.getBytes());
            byte[] decrypted = cipher.doFinal(textBytes);

            realText = new String(decrypted, "UTF-8");
        } catch (Exception e) {
            logger.error("Failed to decrypt", e);
        }
        return realText;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_DEFAULT_ENCODING")
    private static String getKey(String keyLoc)
        throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(keyLoc))) {
            String ln = in.readLine();
            return ln;
        } catch (IOException e) {
            logger.error("Failed to get cryptographic key", e);
            throw e;
        }
    }

    private static boolean isKeyChanged() {
        return keyTimeStamp != new File(keyLocation).lastModified();
    }
}
