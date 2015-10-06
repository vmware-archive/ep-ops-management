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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

public class SecuredPbeStringEncryptor implements PBEStringEncryptor {

    private static final int KEY_LENGTH_IN_BITS = 128;
    private static final int IV_LENGTH_IN_BITS = KEY_LENGTH_IN_BITS / 8;
    private static final int SALT_LENGTH_IN_BITS = KEY_LENGTH_IN_BITS / 8;
    private static final int ITERAIONS_COUNT = 4096;
    private static final String EMPTY_STRING = "";
    private static final Log logger = LogFactory.getLog(SecuredPbeStringEncryptor.class);
    private static final String ENCRYPTION_ALGORITHM = "PBEWithSHA256And128BitAES-CBC-BC";
    private String password;

    static {
        if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public SecuredPbeStringEncryptor(String password) {
        this.setPassword(password);
    }

    /**
     * Encrypts a message. The String returned by this method is BASE64-encoded String. After the encryption, the salt
     * and IV are concatenated to the encrypted data.
     */

    public String encrypt(String data) {
        String operationName = "encryption";
        validatePasswordNotEmpty(operationName);

        byte[] salt = generateRandomsArray(SALT_LENGTH_IN_BITS);
        byte[] iv = generateRandomsArray(IV_LENGTH_IN_BITS);
        byte[] encryptedBytes = null;

        try {
            encryptedBytes = encrypt(convertStringToByteArray(data), password.toCharArray(), salt, iv);
        } catch (Exception e) {
            handleOperationError(operationName);
        }

        // encrypted message includes the salt and IV
        byte[] saltWithIv = ArrayUtils.addAll(salt, iv);
        byte[] encryptedResult = ArrayUtils.addAll(saltWithIv, encryptedBytes);
        return Base64.toBase64String(encryptedResult);

    }

    /**
     * Decrypts a message. This method expects to receive a BASE64-encoded String. Input is expected to have the salt
     * and iv concatenated to the encrypted data.
     */

    public String decrypt(String data) {
        String operationName = "decryption";
        validatePasswordNotEmpty(operationName);

        byte[] dataWithSaltAndIv = Base64.decode(data);
        byte[] decryptedData = null;

        try {
            decryptedData = decrypt(dataWithSaltAndIv, password.toCharArray());
        } catch (Exception e) {
            handleOperationError(operationName);
        }

        if (decryptedData == null || decryptedData.length == 0) {
            return EMPTY_STRING;
        }
        return new String(Base64.decode(Base64.toBase64String(decryptedData)));
    }

    private byte[] generateRandomsArray(int size) {
        byte[] randomsArray = new byte[size];
        SecureRandom randomGen = new SecureRandom();
        randomGen.nextBytes(randomsArray);
        return randomsArray;
    }

    private void handleOperationError(String operationName) {
        String errorMessage = String.format("An error occurred while performing %s", operationName);
        logger.error(errorMessage);
        throw new EncryptionOperationNotPossibleException(errorMessage);
    }

    private void validatePasswordNotEmpty(String operationName) {
        if (StringUtils.isEmpty(password)) {
            throw new EncryptionInitializationException(String.format("Cannot perform %s. Empty password.",
                        operationName));
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private static byte[] convertStringToByteArray(String text)
        throws UnsupportedEncodingException {
        return text.getBytes("UTF-8");
    }

    private static byte[] encrypt(byte[] bytesToEncrypt,
                                  char[] password,
                                  byte[] salt,
                                  byte[] iv)
        throws GeneralSecurityException {

        SecretKeySpec key = generateSecretKeySpec(password, salt);

        Cipher encryptionCipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        return encryptionCipher.doFinal(bytesToEncrypt);
    }

    private static SecretKeySpec generateSecretKeySpec(char[] password,
                                                       byte[] salt)
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, ITERAIONS_COUNT, KEY_LENGTH_IN_BITS);
        SecretKeyFactory secretKeyFact = SecretKeyFactory.getInstance(ENCRYPTION_ALGORITHM);
        SecretKeySpec key = new SecretKeySpec(secretKeyFact.generateSecret(pbeKeySpec).getEncoded(), "AES");
        return key;
    }

    private static byte[] decrypt(final byte[] bytesToDecrypt,
                                  final char[] password)
        throws GeneralSecurityException {

        byte[] salt = ArrayUtils.subarray(bytesToDecrypt, 0, SALT_LENGTH_IN_BITS);
        byte[] iv = ArrayUtils.subarray(bytesToDecrypt, SALT_LENGTH_IN_BITS, SALT_LENGTH_IN_BITS + IV_LENGTH_IN_BITS);
        byte[] encryptedData =
                    ArrayUtils.subarray(bytesToDecrypt, SALT_LENGTH_IN_BITS + IV_LENGTH_IN_BITS, bytesToDecrypt.length);

        SecretKeySpec secretKeySpec = generateSecretKeySpec(password, salt);

        Cipher decryptionCipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        decryptionCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));

        byte[] decryptedData = decryptionCipher.doFinal(encryptedData);

        return decryptedData;
    }

}
