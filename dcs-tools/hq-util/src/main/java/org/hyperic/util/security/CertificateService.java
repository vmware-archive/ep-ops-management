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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public class CertificateService {

    public static final String THUMBPRINT_ALGORITHM = "SHA1";
    public static final String SIGNATURE_ALGORITHM = "SHA512WITHRSA";
    public static final String KEY_ALGORITHM = "RSA";
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    public static final int KEY_SIZE = 2048;
    public static final long CERT_VALIDITY_IN_MILLIS = (((1000L * 60 * 60 * 24 * 30)) * 12) * 10; // 10 years

    private static final String CLIENT_CERTIFICATE_SUFFIX =
                ", OU=Agent, O=VMware\\, Inc.";
    private static final Log log = LogFactory.getLog(CertificateService.class);
    private static final String KEYSTORE_TYPE_JKS = "JKS";
    private static final SecureRandom random = new SecureRandom();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Create internal agent keystore with a generated private key and a self-signed certificate. If the keystore
     * already exists, just add/replace the default hq key pair
     * 
     * @param keystoreConfig
     * @param dName for the self-signed agent certificate
     * @return the new keystore
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws OperatorCreationException
     */
    public static KeyStore createInternalKeystore(KeystoreConfig keystoreConfig,
                                                  String dName)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        File file = new File(keystoreConfig.getFilePath());
        final KeyPair agentKeyPair = generateKeyPair();
        KeyStore keystore = null;
        if (!file.exists()) {
            log.debug("Creating a new keystore at " + keystoreConfig.getFilePath());
            keystore = loadKeystore(keystoreConfig, null/*keystoreConfig*/, KEYSTORE_TYPE_JKS);
            storeKeystore(keystore, keystoreConfig);
        }
        log.debug("Saving generated keypair to the keystore");
        keystore = savePrivateKey(keystoreConfig.getAlias(), dName, agentKeyPair, keystore, keystoreConfig);
        return keystore;
    }

    /**
     * Creates a self signed certificate to be used as a pair for storage with a private key that is used for client
     * certificate generation (after registration this certificate will be replaced by a signed client certificate)
     */
    public static X509Certificate createSelfSignedCertificate(String dn,
                                                              String issuer,
                                                              KeyPair keyPair)
        throws OperatorCreationException, CertIOException, CertificateException {
        PublicKey subjectPublicKey = keyPair.getPublic();
        PrivateKey issuerPrivateKey = keyPair.getPrivate();

        BigInteger serialNumber = BigInteger.valueOf(Math.abs(random.nextInt()));
        X500Name subjectDN = new X500Name(dn);
        X500Name issuerDN = new X500Name(issuer);

        // Validity
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + CERT_VALIDITY_IN_MILLIS);

        SubjectPublicKeyInfo subjPubKeyInfo =
                    new SubjectPublicKeyInfo(ASN1Sequence.getInstance(subjectPublicKey.getEncoded()));

        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuerDN, serialNumber,
                    notBefore, notAfter, subjectDN, subjPubKeyInfo);

        DigestCalculator digCalc =
                    new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
        X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(digCalc);

        certGen.addExtension(Extension.subjectKeyIdentifier, false,
                    x509ExtensionUtils.createSubjectKeyIdentifier(subjPubKeyInfo));

        ContentSigner sigGen =
                    new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER).build(issuerPrivateKey);

        X509CertificateHolder certHolder = certGen.build(sigGen);
        X509Certificate certificate =
                    new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);

        if (log.isDebugEnabled()) {
            log.debug(String.format("generated certificate for %s issued by %s", dn, issuer));
        }
        return certificate;
    }

    /**
     * Converts the given pem-encoded certificate to X509Certificate
     * 
     * @param certificatePemEncoded
     * @return X509Certificate
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws OperatorCreationException
     */
    public static X509Certificate convertPemEncodedCertificateToX509Certificate(String certificatePemEncoded)
        throws GeneralSecurityException, IOException, OperatorCreationException {

        Object decodedCertificate = CertificateService.pemDecode(certificatePemEncoded);
        if (decodedCertificate instanceof X509CertificateHolder) {
            X509CertificateHolder certificateHolder = (X509CertificateHolder) decodedCertificate;
            X509Certificate certificate =
                        new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certificateHolder);
            return certificate;
        } else {
            final String errorMessage = "Failed to convert certificate. Invalid certificate.";
            log.debug(errorMessage);
            throw new GeneralSecurityException(errorMessage);
        }
    }

    /**
     * Saves the given X509 certificate to the keystore, overwriting an existing entry, if exists. Assumes the private
     * key with the specified alias already exists in the keystore, otherwise throws a KeyStoreException
     * 
     * @param alias
     * @param agentKeyPair
     * @param certificatePemEncoded
     * @param keystoreConfig
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws OperatorCreationException
     */
    public static void saveCertificateToKeystore(String alias,
                                                 X509Certificate certificate,
                                                 KeyPair agentKeyPair,
                                                 KeystoreConfig keystoreConfig)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        KeyStore keystore = KeystoreManager.getKeystoreManager().getKeyStore(keystoreConfig);
        if (keystore.containsAlias(alias)) {
            keystore.deleteEntry(alias);
        }
        saveCertificateWithPrivateKey(alias, agentKeyPair.getPrivate(), certificate, keystore, keystoreConfig);
    }

    /**
     * Utility for converting a security artifact to a string
     * 
     * @param obj Object The security artifact: key, certificate, etc.
     * @return String
     * @throws IOException
     * @throws Exception
     */
    public final static String pemEncode(Object obj)
        throws IOException {
        StringWriter sw = new StringWriter();
        PEMWriter pw = new PEMWriter(sw);
        try {
            pw.writeObject(obj);
            pw.flush();
        } finally {
            pw.close();
        }
        return sw.toString();
    }

    public final static Object pemDecode(String str)
        throws IOException {
        StringReader sr = new StringReader(str);
        PEMParser parser = new PEMParser(sr);
        Object readObject = null;
        try {
            readObject = parser.readObject();
        } finally {
            parser.close();
        }
        return readObject;
    }

    public static KeyPair generateKeyPair()
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException,
        NoSuchProviderException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
        keyGenerator.initialize(KEY_SIZE, random);
        KeyPair keyPair = keyGenerator.generateKeyPair();
        return keyPair;
    }

    /**
     * Save the given keypair in the keystore. If keystore is null, it is loaded from filesystem using keystoreConfig
     * 
     * @param dName for which the self-signed certificate will be created
     * @param keyPair
     * @param store
     * @param keystoreConfig
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws OperatorCreationException
     */
    private static KeyStore savePrivateKey(String alias,
                                           String dName,
                                           KeyPair keyPair,
                                           KeyStore store,
                                           KeystoreConfig keystoreConfig)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        PrivateKey privateKey = keyPair.getPrivate();
        Certificate trustCert = createSelfSignedCertificate(dName, dName, keyPair);
        Certificate[] outChain = { trustCert };
        KeyStore.PrivateKeyEntry pkEntry = new KeyStore.PrivateKeyEntry(privateKey, outChain);
        store = saveEntryToDestStore(alias, pkEntry, keystoreConfig, store);
        storeKeystore(store, keystoreConfig);
        return store;
    }

    /**
     * Save the given keypair in the keystore. If keystore is null, it is loaded from filesystem using keystoreConfig
     * 
     * @param alias for which the self-signed certificate will be created
     * @param keyPair
     * @param store
     * @param keystoreConfig
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws OperatorCreationException
     */
    private static KeyStore saveCertificateWithPrivateKey(String alias,
                                                          PrivateKey privateKey,
                                                          Certificate trustCert,
                                                          KeyStore store,
                                                          KeystoreConfig keystoreConfig)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        Certificate[] outChain = { trustCert };
        KeyStore.PrivateKeyEntry pkEntry = new KeyStore.PrivateKeyEntry(privateKey, outChain);
        store = saveEntryToDestStore(alias, pkEntry, keystoreConfig, store);
        storeKeystore(store, keystoreConfig);
        return store;
    }

    public static byte[] generateCSR(String cn,
                                     KeyPair pair)
        throws OperatorCreationException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("generating CSR for CN: " + cn);
        }
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal(getAgentClientCertificateDN(cn)), pair.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);
        ContentSigner signer = csBuilder.build(pair.getPrivate());
        PKCS10CertificationRequest csr = p10Builder.build(signer);

        if (log.isDebugEnabled()) {
            log.debug("Generated CSR for CN: " + cn);
        }

        return csr.getEncoded();
    }

    private static String getAgentClientCertificateDN(String cn) {
        return "CN=" + cn + CLIENT_CERTIFICATE_SUFFIX;
    }

    /**
     * Save entry to keystore
     * 
     * @param alias
     * @param entry
     * @param keystoreConfig
     * @param store if null, then the keystore is loaded from file system
     * @return
     * @throws GeneralSecurityException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private static KeyStore saveEntryToDestStore(String alias,
                                                 Entry entry,
                                                 KeystoreConfig keystoreConfig,
                                                 KeyStore store)

        throws GeneralSecurityException, NoSuchAlgorithmException,
        IOException {
        log.debug("Saving entry to keystore");
        if (null == store) {
            FileInputStream storeInputStream = null;
            storeInputStream = new FileInputStream(keystoreConfig.getFilePath());
            store = loadKeystore(keystoreConfig, storeInputStream, KEYSTORE_TYPE_JKS);
        }

        if (store.containsAlias(alias)) {
            store.deleteEntry(alias);
        }
        PasswordProtection protectionParam = new PasswordProtection(
                    keystoreConfig.getFilePasswordCharArray());
        store.setEntry(alias, entry, protectionParam);
        log.debug("Adding private key entry with alias: " + alias);
        return store;
    }

    /**
     * Loads keystore (creates a new one if storeInputStream is null) and closes the storeInputStream.
     * 
     * @param keystoreConfig
     * @param storeInputStream
     * @param keystoreType
     * @return the loaded keystore
     * @throws KeyStoreException
     * @throws IOException
     */
    private static KeyStore loadKeystore(KeystoreConfig keystoreConfig,
                                         FileInputStream storeInputStream,
                                         String keystoreType)
        throws KeyStoreException, IOException {
        char[] filePassword = keystoreConfig.getFilePasswordCharArray();
        checkFilePasswordExists(filePassword);
        final KeyStore store = KeyStore.getInstance(keystoreType);
        try {
            store.load(storeInputStream, filePassword);
            log.debug("Loaded keystore");
        } catch (Exception e) {
            log.error("Failed to load keystore, ", e);
        } finally {
            if (storeInputStream != null) {
                storeInputStream.close();
            }
        }
        return store;
    }

    private static void storeKeystore(KeyStore store,
                                      KeystoreConfig keystoreConfig)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        char[] filePassword = keystoreConfig.getFilePasswordCharArray();
        checkFilePasswordExists(filePassword);
        // store away the keystore
        java.io.FileOutputStream fos = null;
        try {
            fos = new java.io.FileOutputStream(keystoreConfig.getFilePath());
            store.store(fos, filePassword);
            log.debug("Stored keystore");
        } catch (CertificateException e) {
            log.error("Failed to store Certificate ", e);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    private static void checkFilePasswordExists(char[] filePassword) {
        if (ArrayUtils.isEmpty(filePassword)) {
            log.error("No keystore password provided.");
            throw new NullArgumentException("keystoreConfig.filePasswordCharArray");
        }
    }

    public static String extractAgentTokenFromCertificate(X509Certificate certificate) {
        String token = "";
        X500Principal principal = certificate.getSubjectX500Principal();
        if (principal == null) {
            return null;
        }

        String subject = principal.getName();
        LdapName certificateNames;

        try {
            certificateNames = new LdapName(subject);
        } catch (InvalidNameException e) {
            log.error("Invalid certificate");
            return null;
        }

        for (Rdn rdn : certificateNames.getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                token = rdn.getValue().toString();
                break;
            }
        }
        return token;
    }

    public static String printCert(X509Certificate x509Certificate) {
        return "\nCertificate Thumbprint(" + THUMBPRINT_ALGORITHM + "): " + getDecoratedSHA1Thumbprint(x509Certificate)
                    +
                    "\nIssued To: " + x509Certificate.getSubjectDN() +
                    "\nIssued By: " + x509Certificate.getIssuerDN() +
                    "\nExpires: " + x509Certificate.getNotAfter();
    }

    /**
     * Returns the sha-1 hash of the given certificate encoding This is returned as a 59 character string (for 20 bytes
     * of Sha-1) For example: 6B:80:A6:5B:96:46:61:D3:B7:BB:34:75:1A:2B:3B:D3:D3:D1:10:22
     */
    public static String getDecoratedSHA1Thumbprint(X509Certificate x509Certificate) {
        StringBuilder builder;

        try {
            byte[] encodedCertificate = x509Certificate.getEncoded();
            MessageDigest digest = MessageDigest.getInstance(THUMBPRINT_ALGORITHM);
            digest.update(encodedCertificate);
            builder = new StringBuilder(59);
            String prefix = "";
            for (byte b : digest.digest()) {
                builder.append(prefix);
                builder.append(String.format("%02X", b));
                prefix = ":";
            }
        } catch (Exception ex) {
            return "Unable to compute thumbprint.";
        }
        return builder.toString();
    }

    /**
     * Compare the given certificate to the given case insensitive thumbprint, ignoring colon/space separators.
     * Supported algorithms (determined by thumbprint length): SHA1/SHA256 algorithms
     */
    public static boolean compareCertificateToThumbprint(X509Certificate certificate,
                                                         String serverCertificateThumbprint) {
        if (serverCertificateThumbprint == null) {
            return false;
        }
        String strippedThumbprint = getStrippedThumbprint(serverCertificateThumbprint);
        return strippedThumbprint.equals(getCertificateThumbprintByLength(certificate, strippedThumbprint.length()));
    }

    /**
     * Returns the given certificate thumbprint in lower case and with no space/colon separators
     */
    private static String getStrippedThumbprint(String serverCertificateThumbprint) {
        return serverCertificateThumbprint.toLowerCase().replaceAll(":", "").replaceAll(" ", "");
    }

    private static String getCertificateThumbprintByLength(X509Certificate certificate,
                                                           int thumbprintHexBytesNum) {
        try {
            switch (thumbprintHexBytesNum) {
                case 40:
                    return DigestUtils.shaHex(certificate.getEncoded());
                case 64:
                    return DigestUtils.sha256Hex(certificate.getEncoded());
                default:
                    return null;
            }
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    /**
     * 
     * Returns subject principal of the certificate aliased in the keystore configuration, Or null if the principal
     * could not be fetched.
     * 
     * */
    public static X500Principal getClientCertificateSubjectPrincipal(KeystoreConfig keyConfig) {
        try {
            KeyStore keystore = KeystoreManager.getKeystoreManager().getKeyStore(keyConfig);
            String alias = keyConfig.getClientCertificateAlias();
            X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
            return cert.getSubjectX500Principal();

        } catch (Exception e) {
            return null;

        }
    }

}
