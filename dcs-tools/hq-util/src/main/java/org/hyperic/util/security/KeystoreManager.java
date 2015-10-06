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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.operator.OperatorCreationException;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.util.StringUtils;

public class KeystoreManager {
    private static final int SLEEP_CYCLE_LENGTH_TO_AWAIT_KEYSTORE = 1000; // ms
    private static final int NUM_SLEEP_CYCLES_TO_AWAIT_KEYSTORE = 20;
    private final AtomicBoolean isDB = new AtomicBoolean(false);
    private static KeystoreManager keystoreManager = new KeystoreManager();
    private final static Log log = LogFactory.getLog(KeystoreManager.class);

    public static KeystoreManager getKeystoreManager() {
        return keystoreManager;
    }

    private String getDName(KeystoreConfig keystoreConfig) {
        return "CN="
                    + keystoreConfig.getKeyCN()
                    +
                    " (Self-Signed Cert), OU=End Point Operations Management , O=vmware.com, L=Unknown, ST=Unknown, C=US";
    }

    public void initializeKeyStore(KeystoreConfig keystoreConfig)
        throws KeyStoreException, IOException {
        log.debug("Initializing keystore");
        validateKeystoreConfig(keystoreConfig);

        FileInputStream keyStoreFileInputStream = null;
        String filePath = keystoreConfig.getFilePath();
        String filePassword = keystoreConfig.getFilePassword();
        String errorMsg;

        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            File file = new File(filePath);
            char[] password = null;

            if (!file.exists()) {
                // ...if file doesn't exist, and path was user specified throw IOException...
                if (StringUtils.hasText(filePath) && !keystoreConfig.isHqDefault()) {
                    throw new IOException("User specified keystore [" + filePath + "] does not exist.");
                }
                password = filePassword.toCharArray();
                keystore = createInternalKeystore(keystoreConfig);
                FileUtil.setReadWriteOnlyByOwner(file);
            } else {
                // ...keystore exists, so init the file input stream...
                keyStoreFileInputStream = new FileInputStream(file);
                keystore.load(keyStoreFileInputStream, password);
                // if private key has not been generated yet in this keystore,
                // generate it
                if (null == fetchPrivateKey(keystoreConfig, keystore)) {
                    keystore = createInternalKeystore(keystoreConfig);
                }
                log.debug("Loading existing keystore");
            }
        } catch (NoSuchAlgorithmException e) {
            // can't check integrity of keystore, if this happens we're kind of screwed
            // is there anything we can do to self heal this problem?
            errorMsg = "The algorithm used to check the integrity of the keystore cannot be found.";
            throw new KeyStoreException(errorMsg, e);
        } catch (CertificateException e) {
            // there are some corrupted certificates in the keystore, a bad thing
            // is there anything we can do to self heal this problem?
            errorMsg = "Keystore cannot be loaded. One possibility is that the password is incorrect.";
            throw new KeyStoreException(errorMsg, e);
        } catch (UnrecoverableEntryException e) {
            errorMsg = "Keystore entry cannot be recovered.";
            throw new KeyStoreException(errorMsg, e);
        } finally {
            if (keyStoreFileInputStream != null) {
                keyStoreFileInputStream.close();
                keyStoreFileInputStream = null;
            }
        }
    }

    /**
     * Get keystore. If it is not initialized yet - wait for it to become initialized.
     * 
     * @throws KeyStoreException
     * @throws IOException
     */
    public KeyStore getKeyStore(KeystoreConfig keystoreConfig)
        throws KeyStoreException, IOException {
        KeyStore keystore = waitForKeystoreFileInitialization(keystoreConfig);
        return keystore;
    }

    /**
     * Wait for keystore to be initialized in another process/thread. The keystore is considered initialized if it
     * exists and contains the private key entry with the alias keystoreConfig.getAlias()
     * 
     * @throws KeyStoreException
     * @throws IOException
     */
    private KeyStore waitForKeystoreFileInitialization(KeystoreConfig keystoreConfig)
        throws KeyStoreException, IOException {
        String filePath = keystoreConfig.getFilePath();
        File keystoreFile = new File(filePath);
        int numRetriesLeft = NUM_SLEEP_CYCLES_TO_AWAIT_KEYSTORE;
        String errorMsg;
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            boolean isKeystoreInitialized;
            do {
                isKeystoreInitialized = isKeystoreInitialized(keystoreFile, keystore, keystoreConfig);
                if (!isKeystoreInitialized) {
                    try {
                        log.debug("Waiting for the keystore to be initialized");
                        Thread.sleep(SLEEP_CYCLE_LENGTH_TO_AWAIT_KEYSTORE);
                    } catch (InterruptedException ignore) { /* ignore */
                    }
                }
                --numRetriesLeft;
            } while ((numRetriesLeft > 0) && !isKeystoreInitialized);

            if (!isKeystoreInitialized) {
                throw new KeyStoreException(String.format("The keystore at %s is not initialized. Cannot continue.",
                            filePath));
            }
            log.debug("Loaded an initialized keystore");

            return keystore;
        } catch (NoSuchAlgorithmException e) {
            // can't check integrity of keystore, if this happens we're kind of screwed
            // is there anything we can do to self heal this problem?
            errorMsg = "The algorithm used to check the integrity of the keystore cannot be found.";
            throw new KeyStoreException(errorMsg, e);
        } catch (CertificateException e) {
            // there are some corrupted certificates in the keystore, a bad thing
            // is there anything we can do to self heal this problem?
            errorMsg = "Keystore cannot be loaded. One possibility is that the password is incorrect.";
            throw new KeyStoreException(errorMsg, e);
        } catch (UnrecoverableEntryException e) {
            errorMsg = "Keystore cannot be loaded. "
                        + "One possibility is that the alias defined in property file is incorrect.";
            throw new KeyStoreException(errorMsg, e);
        } catch (IOException e) {
            errorMsg = "Keystore cannot be loaded. ";
            throw new KeyStoreException(errorMsg, e);
        }
    }

    private static boolean isKeystoreInitialized(File keystoreFile,
                                                 KeyStore keystore,
                                                 KeystoreConfig keystoreConfig)
        throws NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableEntryException,
        KeyStoreException {
        boolean isKeystoreInitialized = false;
        if (keystoreFile.exists()) {
            FileInputStream keyStoreFileInputStream = null;
            try {
                // ...keystore exists, so init the file input stream...
                keyStoreFileInputStream = new FileInputStream(keystoreFile);
                keystore.load(keyStoreFileInputStream, null/*password*/);
                // fetch the private key to make sure the keystore is initialized
                KeyStore.Entry privateKeyEntry = fetchPrivateKey(keystoreConfig, keystore);
                isKeystoreInitialized = (null != privateKeyEntry);
            } finally {
                if (keyStoreFileInputStream != null) {
                    keyStoreFileInputStream.close();
                    keyStoreFileInputStream = null;
                }
            }
        }
        return isKeystoreInitialized;
    }

    /**
     * Fetch the private key entry with the alias as specified in keystoreConfig.getAlias(), or null if the key could
     * not be retrieved.
     * 
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableEntryException
     * @throws KeyStoreException
     */
    private static KeyStore.Entry fetchPrivateKey(KeystoreConfig keystoreConfig,
                                                  KeyStore keystore)
        throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        try {
            return keystore.getEntry(keystoreConfig.getAlias(), new KeyStore.PasswordProtection(keystoreConfig
                        .getFilePassword().toCharArray()));
        } catch (IOException e) {
            return null;
        }
    }

    private void validateKeystoreConfig(KeystoreConfig keystoreConfig)
        throws KeyStoreException {
        if (null == keystoreConfig) {
            throw new KeyStoreException("Received null keystoreConfig");
        }

        String errorMsg = "";
        if (keystoreConfig.getAlias() == null) {
            errorMsg += " alias is null. ";
        }
        if (keystoreConfig.getFilePath() == null) {
            errorMsg += " filePath is null. ";
        }
        try {
            if (keystoreConfig.getFilePassword() == null) {
                errorMsg += " password is null. ";
            }
        } catch (IOException e) {
            errorMsg += " couldn't get password. ";
        }
        if (!"".equals(errorMsg)) {
            throw new KeyStoreException(errorMsg);
        }
    }

    private KeyStore createInternalKeystore(KeystoreConfig keystoreConfig)
        throws KeyStoreException {
        KeyStore keystore;
        try {
            String dName = getDName(keystoreConfig);
            keystore = CertificateService.createInternalKeystore(keystoreConfig, dName);
        } catch (IOException e) {
            String errorMsg = "Failed to save the keystore.";
            throw new KeyStoreException(errorMsg, e);
        } catch (OperatorCreationException e) {
            String errorMsg = "Failed to save the keystore.";
            throw new KeyStoreException(errorMsg, e);
        } catch (GeneralSecurityException e) {
            String errorMsg = "Failed to save the keystore.";
            throw new KeyStoreException(errorMsg, e);
        }
        return keystore;
    }

    public X509TrustManager getCustomTrustManager(X509TrustManager defaultTrustManager,
                                                  KeystoreConfig keystoreConfig,
                                                  boolean acceptUnverifiedCertificates,
                                                  KeyStore trustStore) {
        return new CustomTrustManager(defaultTrustManager, keystoreConfig,
                    acceptUnverifiedCertificates, trustStore, isDB.get());
    }

    public static X509TrustManager getCustomTrustManager(KeystoreConfig keystoreConfig)
        throws KeyStoreException, IOException {
        KeystoreManager keystoreMgr = KeystoreManager.getKeystoreManager();
        KeyStore trustStore = keystoreMgr.getKeyStore(keystoreConfig);
        X509TrustManager defaultTrustManager = KeystoreManager.getDefaultTrustManager(trustStore);
        X509TrustManager customTrustManager =
                    keystoreMgr.getCustomTrustManager(defaultTrustManager, keystoreConfig, false, trustStore);
        return customTrustManager;
    }

    private class CustomTrustManager implements X509TrustManager {
        private final Log log = LogFactory.getLog(X509TrustManager.class);
        private final X509TrustManager defaultTrustManager;
        private final KeystoreConfig keystoreConfig;
        private final boolean acceptUnverifiedCertificates;
        private final KeyStore trustStore;
        private final boolean isDB;

        private CustomTrustManager(X509TrustManager defaultTrustManager,
                                   KeystoreConfig keystoreConfig,
                                   boolean acceptUnverifiedCertificates,
                                   KeyStore trustStore,
                                   boolean isDB) {
            this.defaultTrustManager = defaultTrustManager;
            this.keystoreConfig = keystoreConfig;
            this.acceptUnverifiedCertificates = acceptUnverifiedCertificates;
            this.trustStore = trustStore;
            this.isDB = isDB;
        }

        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType)
            throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                for (X509Certificate certificate : chain) {
                    certificate.checkValidity();
                }
            } catch (CertificateException e) {
                CertificateException expiredCertException =
                            getCertificateExceptionOfType(e, CertificateExpiredException.class);
                if (expiredCertException != null && !acceptUnverifiedCertificates) {
                    log.error("Fail the connection because the received certificate is expired. " +
                                "Please update the certificate.", expiredCertException);
                    throw expiredCertException;
                }
                CertificateException notYetValidCertException =
                            getCertificateExceptionOfType(e, CertificateNotYetValidException.class);
                if (notYetValidCertException != null && !acceptUnverifiedCertificates) {
                    log.error("Fail the connection because the received certificate is not valid yet.",
                                notYetValidCertException);
                    throw notYetValidCertException;
                }
                if (acceptUnverifiedCertificates) {
                    log.debug("Allowing the connection. Received certificate was not trusted by keystore, " +
                                "but allowed by property ");
                } else {
                    log.warn("Fail the connection because the received certificate is not trusted");
                    log.debug("Fail the connection because the received certificate is not trusted. " +
                                "acceptUnverifiedCertificates=" + acceptUnverifiedCertificates, e);
                    throw new CertificateException(e);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends CertificateException> T getCertificateExceptionOfType(Exception exception,
                                                                                 Class<? extends CertificateException> searchedClass) {
            while (exception != null) {
                if (searchedClass.isInstance(exception)) {
                    return (T) exception;
                }
                exception = (Exception) exception.getCause();
            }
            return null;
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType)
            throws CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        public void importServerCertificate(X509Certificate x509Certificate,
                                            String serverCertificateAlias)
            throws CertificateException {
            FileOutputStream ksFileOutputStream = null;
            final boolean debug = log.isDebugEnabled();
            final StopWatch watch = new StopWatch();
            try {
                trustStore.setCertificateEntry(serverCertificateAlias, x509Certificate);
                if (!isDB) {
                    ksFileOutputStream = new FileOutputStream(keystoreConfig.getFilePath());
                    trustStore.store(ksFileOutputStream, keystoreConfig.getFilePassword().toCharArray());
                }
            } catch (FileNotFoundException e) {
                // Can't find the keystore in the path
                log.error("Can't find the keystore in " + keystoreConfig.getFilePath() +
                            ". Error message: " + e, e);
            } catch (NoSuchAlgorithmException e) {
                log.error("The algorithm is not supported. Error message: " + e, e);
            } catch (Exception e) {
                // expect KeyStoreException, IOException
                log.error("Exception when trying to import certificate: " + e, e);
            } finally {
                close(ksFileOutputStream);
                ksFileOutputStream = null;
                if (debug)
                    log.debug("importCert: " + watch);
            }
        }

        private void close(FileOutputStream fos) {
            if (fos == null) {
                return;
            }
            try {
                fos.close();
            } catch (IOException e) {
            }
        }
    }

    public void importServerCert(KeystoreConfig keystoreConfig,
                                 X509Certificate x509Certificate) {

        try {
            String serverCertificateAlias = keystoreConfig.getServerCertificateAlias();
            KeyStore trustStore = getKeyStore(keystoreConfig);
            CustomTrustManager myCustomTrustManager =
                        new CustomTrustManager(null, keystoreConfig, true, trustStore, false);
            myCustomTrustManager.importServerCertificate(x509Certificate, serverCertificateAlias);
        } catch (Exception e) {
            log.error("Failed to import X509Certificate", e);
        }
    }

    public static X509TrustManager getDefaultTrustManager(KeyStore trustStore)
        throws KeyStoreException, IOException {
        TrustManagerFactory trustManagerFactory = getTrustManagerFactory(trustStore);
        return (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
    }

    private static TrustManagerFactory getTrustManagerFactory(final KeyStore keystore)
        throws KeyStoreException, IOException {
        try {
            TrustManagerFactory trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            return trustManagerFactory;
        } catch (NoSuchAlgorithmException e) {
            // no support for algorithm, if this happens we're kind of screwed
            // we're using the default so it should never happen
            log.error("The algorithm is not supported: " + e, e);
            throw new KeyStoreException(e);
        }
    }
}
