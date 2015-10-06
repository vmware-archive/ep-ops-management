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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CertificateServiceTest {

    private final String cn = "some-token";
    private final String dName = "C=AU,ST=Victoria";
    private final String filePath = "testCreateInternalKeystore.temp";
    private final String filePassword = "password";
    private final String alias = "some-alias";
    private final String clientCertificateAlias = "some-other-alias";
    private final String RSA = CertificateService.KEY_ALGORITHM;
    private final String X509 = "X.509";
    private final String SIG_ALGORITHM = CertificateService.SIGNATURE_ALGORITHM;

    @Before
    public void setUp()
        throws Exception {
    }

    @After
    public void tearDown()
        throws Exception {
        File file = new File(filePath);
        file.delete();
    }

    @Test
    public void testCreateInternalKeystore()
        throws OperatorCreationException,
        GeneralSecurityException, IOException {

        KeystoreConfig keystoreConfig = makeKeystoreConfig(filePath,
                    filePassword, alias, clientCertificateAlias);
        KeyStore keyStore = CertificateService.createInternalKeystore(
                    keystoreConfig, dName);

        Assert.assertEquals(1, keyStore.size());
        X509Certificate certificate = (X509Certificate) keyStore
                    .getCertificateChain(alias)[0];

        checkCertificateInfo(certificate);
    }

    private void checkCertificateInfo(X509Certificate certificate) {
        String actual = certificate.getIssuerDN().getName();
        Assert.assertEquals("DN does not match", dName, actual);

        Principal actualDn = certificate.getSubjectDN();
        Assert.assertEquals(dName, actualDn.toString());

        String expected = RSA;
        actual = certificate.getPublicKey().getAlgorithm();
        Assert.assertEquals("Unexpected key algorithm", expected, actual);

        expected = X509;
        actual = certificate.getPublicKey().getFormat();
        Assert.assertEquals("Unexpected format", expected, actual);

        expected = SIG_ALGORITHM;
        actual = certificate.getSigAlgName();
        Assert.assertEquals("Unexpected signature algorithm", expected, actual);
    }

    @Test
    public void testGenerateCSR()
        throws OperatorCreationException,
        GeneralSecurityException, IOException {

        KeystoreConfig keystoreConfig = makeKeystoreConfig(filePath,
                    filePassword, alias, clientCertificateAlias);
        CertificateService.createInternalKeystore(keystoreConfig, dName);

        KeyPair keyPair = CertificateService.generateKeyPair();
        byte[] certificateRequest = CertificateService.generateCSR(cn, keyPair);

        PKCS10CertificationRequest request = new PKCS10CertificationRequest(
                    certificateRequest);

        X500Name subjectDN = request.getSubject();
        String expected = "O=VMware\\, Inc.,OU=Agent,CN=some-token";
        Assert.assertEquals(expected, subjectDN.toString());
    }

    @Test
    public void testCreateSelfSignedCertificate()
        throws OperatorCreationException, CertIOException,
        CertificateException, NoSuchAlgorithmException,
        NoSuchProviderException {

        KeyPair keyPair = generateKeyPair();

        X509Certificate certificate = CertificateService
                    .createSelfSignedCertificate(dName, dName, keyPair);

        Assert.assertEquals(keyPair.getPublic(), certificate.getPublicKey());

        checkCertificateInfo(certificate);
    }

    private KeystoreConfig makeKeystoreConfig(String filePath,
                                              String filePassword,
                                              String alias,
                                              String clientCertificateAlias) {
        KeystoreConfig keystoreConfig = new KeystoreConfig();
        keystoreConfig.setFilePath(filePath);
        keystoreConfig.setFilePassword(filePassword);
        keystoreConfig.setAlias(alias);
        keystoreConfig.setClientCertificateAlias(clientCertificateAlias);

        return keystoreConfig;
    }

    private KeyPair generateKeyPair()
        throws NoSuchAlgorithmException,
        NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(RSA,
                    BouncyCastleProvider.PROVIDER_NAME);
        keyGenerator.initialize(CertificateService.KEY_SIZE, new SecureRandom());
        return keyGenerator.generateKeyPair();
    }
}
