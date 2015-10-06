package org.hyperic.hq.agent;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.PropertyEncryptionUtil;
import org.hyperic.util.PropertyUtilException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.security.SecurityUtil;

/**
 * A service for managing keystore credentials using local file system. Keystore passwords are generated if needed, and
 * stored in a (.pwd) file near their keystore file.
 */
public enum KeystoreCredentialsManager {
    INSTANCE;

    private static final Log log = LogFactory.getLog(KeystoreCredentialsManager.class);
    private static final String PWD_FILE_EXT = ".pwd";
    private static final SecureRandom random = new SecureRandom();
    private static final int NUM_OF_GEN_PWD_DIGITS = 12;
    private static final int PASSWORD_MAX_LENGTH = 120;
    private static final int CIPHER_MAX_LENGTH = 221;
    private static final String PASSWORD_ENCODING = CharEncoding.UTF_8;
    private final KeystoreCredentialCache cache;
    private char[] encryptionKey = null;

    private KeystoreCredentialsManager() {
        cache = new KeystoreCredentialCache();
    }

    public static KeystoreCredentialsManager getInstance() {
        return INSTANCE;
    }

    public synchronized char[] getKeystorePassword(File keystore,
                                                   char[] userDefaultPassword)
        throws IOException {
        char[] cachedPassword = cache.get(keystore);
        if (cachedPassword == null) {
            log.debug("Keystore password not in cache, fetching from file.");
            char[] fetchedPassword = fetchKeystorePassword(keystore);
            if (fetchedPassword == null) {
                log.debug("Keystore password not in file (or invalid). Creating password file.");
                char[] storedPassword = createKeystorePassword(keystore, userDefaultPassword);
                cache.put(keystore, storedPassword);
                return storedPassword;
            } else {
                cache.put(keystore, fetchedPassword);
                return fetchedPassword;
            }
        }

        return cachedPassword;
    }

    private char[] createKeystorePassword(File keystore,
                                          char[] password)
        throws IOException {
        if (AgentConfig.AUTO_GENERATED_KEYSTORE_MARKER.equals(String.valueOf(password))) {
            log.info("Generating random password for keystore: " + keystore.getPath());
            return writeKeystorePassword(keystore, generatePassword());
        }

        if (isValid(String.valueOf(password))) {
            log.info("Encrypting given password before writing it to file.");
            writeKeystorePassword(keystore, encrypt(password));
            return password;
        } else {
            log.warn("Password is invalid, generating a new one.");
            return writeKeystorePassword(keystore, generatePassword());
        }
    }

    private char[] fetchKeystorePassword(File keystore)
        throws IOException {
        File keystorePasswordFile = getKeystorePasswordFile(keystore);
        try {
            if (keystorePasswordFile.exists()) {
                log.debug("Reading keystore password from file: " + keystorePasswordFile.getPath());
                String fetchedPassword = FileUtils.readFileToString(keystorePasswordFile, PASSWORD_ENCODING);

                String password = null;
                if (SecurityUtil.isMarkedEncrypted(fetchedPassword)) {
                    if (isCipherValid(fetchedPassword)) {
                        String decryptedPassword = SecurityUtil.decrypt(fetchEncryptionKey(), fetchedPassword);
                        password = decryptedPassword;
                    }
                } else {
                    password = fetchedPassword;
                }

                if (isValid(password)) {
                    return password.toCharArray();
                }
            }
        } catch (IOException e) {
            throw loggedIOException("Couldn't read keystore password from file: " + keystorePasswordFile.getPath(), e);
        } catch (PropertyUtilException e) {
            throw loggedIOException("Couldn't decrypt keystore password", e);
        }

        return null;
    }

    private char[] writeKeystorePassword(File keystore,
                                         char[] password)
        throws IOException {
        File keystorePassword = getKeystorePasswordFile(keystore);
        try {
            log.debug("Writing keystore password to file: " + keystorePassword.getPath());
            FileUtils.writeStringToFile(getKeystorePasswordFile(keystore), String.valueOf(password), PASSWORD_ENCODING);
            return password;
        } catch (IOException e) {
            throw loggedIOException("Couldn't write keystore password to file: " + keystorePassword.getPath(), e);
        }
    }

    private char[] generatePassword() {
        return new BigInteger(5 * NUM_OF_GEN_PWD_DIGITS, random).toString(32).toCharArray();
    }

    private File getKeystorePasswordFile(File keystore) {
        File pwdfile = new File(keystore.getParent(), keystore.getName() + PWD_FILE_EXT);

        return pwdfile;
    }

    private boolean isValid(String password) {
        if (StringUtil.isNullOrEmpty(password) || password.length() > PASSWORD_MAX_LENGTH) {
            return false;
        }

        return true;
    }

    private boolean isCipherValid(String cipher) {
        return cipher.length() <= CIPHER_MAX_LENGTH;
    }

    private String fetchEncryptionKey()
        throws PropertyUtilException {
        if (encryptionKey == null) {
            encryptionKey = PropertyEncryptionUtil.getPropertyEncryptionKey(AgentConfig.PROP_ENC_KEY_FILE[1])
                        .toCharArray();
        }

        return String.valueOf(encryptionKey);
    }

    private char[] encrypt(char[] password) {
        try {
            return SecurityUtil.encrypt(fetchEncryptionKey(), String.valueOf(password)).toCharArray();
        } catch (PropertyUtilException e) {
            log.warn("Couldn't encrypt keystore password. Continuing.", e);
            return password;
        }
    }

    private IOException loggedIOException(String msg,
                                          Exception e)
        throws IOException {
        log.error(msg, e);
        return new IOException(msg, e);
    }

    private static class KeystoreCredentialCache {
        private final Map<File, char[]> credentials;

        private KeystoreCredentialCache() {
            credentials = new HashMap<File /* keystore */, char[] /* password */>(1);
        }

        private char[] get(File keystore) {
            if (credentials.containsKey(keystore)) {
                log.debug("Retrieved cached password for keystore: " + keystore.getPath());
                return credentials.get(keystore);
            }

            return null;
        }

        private void put(File keystore,
                         char[] password) {
            credentials.put(keystore, password);
        }
    }
}
