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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import junit.framework.Assert;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.junit.Before;
import org.junit.Test;

import com.vmware.epops.util.security.CertificateHandler;

/**
 * Unit + integration tests for CertificateHandler class
 * 
 * @author serebrob
 */
public class CertificateHandlerTest {
    private static String ISSUER_STRING =
                "CN=Test Issuer CN,OU=Test Issuer OU,O=Test Issuer O,L=Test Issuer L,ST=Test Issuer ST,C=Test Issuer C";
    X500Name issuerName = new X500Name(ISSUER_STRING);

    private static final SecureRandom random = new SecureRandom();
    private static int KEYSIZE = 2048;

    private static byte[] SIGNED_CERTIFICATE_BYTES = {
                48, -126, 3, 123, 48, -126, 2, 99, 2, 4, 25, -17, -43, -81, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13,
                1, 1, 5, 5, 0, 48, -127, -109, 49, 23, 48, 21, 6, 3, 85, 4, 3, 12, 14, 84, 101, 115, 116, 32, 73, 115,
                115, 117, 101, 114, 32, 67, 78, 49, 23, 48, 21, 6, 3, 85, 4, 11, 12, 14, 84, 101, 115, 116, 32, 73,
                115, 115, 117, 101, 114, 32, 79, 85, 49, 22, 48, 20, 6, 3, 85, 4, 10, 12, 13, 84, 101, 115, 116, 32,
                73, 115, 115, 117, 101, 114, 32, 79, 49, 22, 48, 20, 6, 3, 85, 4, 7, 12, 13, 84, 101, 115, 116, 32, 73,
                115, 115, 117, 101, 114, 32, 76, 49, 23, 48, 21, 6, 3, 85, 4, 8, 12, 14, 84, 101, 115, 116, 32, 73,
                115, 115, 117, 101, 114, 32, 83, 84, 49, 22, 48, 20, 6, 3, 85, 4, 6, 19, 13, 84, 101, 115, 116, 32, 73,
                115, 115, 117, 101, 114, 32, 67, 48, 30, 23, 13, 49, 52, 48, 54, 49, 53, 50, 51, 49, 51, 50, 51, 90,
                23, 13, 50, 52, 48, 52, 50, 51, 50, 51, 49, 51, 50, 51, 90, 48, 112, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19,
                2, 73, 76, 49, 18, 48, 16, 6, 3, 85, 4, 8, 19, 9, 65, 103, 101, 110, 116, 76, 97, 110, 100, 49, 17, 48,
                15, 6, 3, 85, 4, 7, 19, 8, 65, 103, 101, 110, 116, 65, 73, 77, 49, 33, 48, 31, 6, 3, 85, 4, 10, 19, 24,
                65, 108, 32, 77, 117, 115, 116, 97, 107, 97, 98, 97, 108, 32, 112, 117, 98, 108, 105, 115, 104, 105,
                110, 103, 49, 23, 48, 21, 6, 3, 85, 4, 3, 12, 14, 80, 85, 84, 95, 84, 79, 75, 69, 78, 95, 72, 69, 82,
                69, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48,
                -126, 1, 10, 2, -126, 1, 1, 0, -115, 110, -10, 51, -16, -72, 52, 89, -102, 89, 95, -22, -62, -22, 58,
                -89, -31, -15, -31, 83, -35, -86, 62, -124, -32, -83, 67, -9, 43, -32, 98, 107, -16, 83, 69, 36, 56,
                25, 84, -8, -99, 40, 48, 18, -89, -107, 105, 1, 14, -2, -60, -48, -94, -24, -98, -116, -19, -38, -19,
                -6, -1, 25, -29, -63, 89, 111, -22, -71, -82, -43, -80, -67, 102, 124, -26, 74, -48, 78, 45, -94, 54,
                75, 85, 53, 59, 46, 55, 12, -29, -126, 52, -98, 62, 110, 13, -109, -52, 112, 65, 104, -18, -58, -69,
                94, -8, 43, -3, 43, 39, -101, 85, 32, 49, 101, -76, -69, 115, -102, -104, -49, -119, -10, -34, -17, 14,
                -43, -93, 42, -28, 68, 4, -125, -11, 39, -54, -44, 101, -25, -11, -104, -128, -53, -19, 2, -25, -66,
                -76, -110, 93, -76, 72, 58, -102, -103, -66, -60, 116, 73, 40, 32, 67, 57, -49, 82, 102, 28, -88, -78,
                -65, -2, -103, -69, 39, 107, -15, -98, 62, -22, -121, 21, -6, 121, -56, -86, 32, -20, -89, 81, 77, 88,
                43, 36, -102, -78, 87, 27, 79, 20, 105, 125, -110, 13, -112, 26, -20, -53, -119, 92, -17, 89, 18, -65,
                -22, -68, 115, -106, -108, 70, -76, -26, -12, 118, -92, 49, -41, -116, -39, -115, -7, 27, 125, 26, 106,
                20, -117, 4, 125, -31, 37, -103, 97, 21, -15, 2, 22, 56, 66, 112, 68, -26, -49, -42, 121, -18, 106, 3,
                2, 3, 1, 0, 1, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 5, 5, 0, 3, -126, 1, 1, 0, 6, 38, 32,
                79, -94, -111, 126, 6, 101, 61, -95, -125, -75, -53, 50, 65, 7, -14, 21, -86, 17, -61, -109, 21, 44,
                -96, 58, 4, -93, 13, 90, -77, 108, -76, -75, 2, -80, -37, -6, -31, 48, -11, 104, -7, 68, -54, 113, 116,
                94, -22, -124, -9, 67, 14, 10, -39, 108, -62, -81, -75, -119, 118, -30, 68, 96, -6, -3, -13, 68, 109,
                -70, 80, -91, -60, -121, 52, -27, 114, 103, 58, -16, 125, 117, 48, 18, -103, -48, 21, -95, -120, -69,
                26, 10, 92, 63, 35, 71, 46, -41, 60, 64, -57, -41, -46, -68, 25, 81, -70, 75, 82, -19, 54, 23, -30, -5,
                30, 92, -65, -109, 49, 47, -40, 79, -115, 54, -62, 99, -32, -62, -116, 3, -61, 37, -101, -40, 68, 28,
                -84, 39, 23, -47, -126, -5, 89, -4, 127, 65, 82, -86, -93, -20, 49, -57, -54, -101, -35, -113, 122, 6,
                65, -40, 13, 51, 110, 86, -34, -110, -62, -85, 64, -74, -20, 87, -10, 43, 120, 18, 84, -103, -66, -102,
                -99, -33, -127, -71, 53, -44, 84, 57, -19, 100, 18, -3, 116, -125, -11, -73, 96, 122, 63, -44, -2, -6,
                37, -33, -107, -100, 8, 56, -50, -110, -49, -74, -3, -116, -74, -48, 42, 58, -48, 30, -56, -124, -11,
                81, 91, 102, 102, 21, -98, 79, -57, -124, -11, 28, -87, 65, -89, -124, -83, -115, 115, -15, 1, -24,
                -79, -92, -19, -38, 83, 57, -64, -32, 58, -66, 116 };

    private static X509Certificate signedCertificate = null;

    private static String PEM_ENCODED_SIGNED_CERT = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDezCCAmMCBBnv1a8wDQYJKoZIhvcNAQEFBQAwgZMxFzAVBgNVBAMMDlRlc3Qg\n" +
                "SXNzdWVyIENOMRcwFQYDVQQLDA5UZXN0IElzc3VlciBPVTEWMBQGA1UECgwNVGVz\n" +
                "dCBJc3N1ZXIgTzEWMBQGA1UEBwwNVGVzdCBJc3N1ZXIgTDEXMBUGA1UECAwOVGVz\n" +
                "dCBJc3N1ZXIgU1QxFjAUBgNVBAYTDVRlc3QgSXNzdWVyIEMwHhcNMTQwNjE1MjMx\n" +
                "MzIzWhcNMjQwNDIzMjMxMzIzWjBwMQswCQYDVQQGEwJJTDESMBAGA1UECBMJQWdl\n" +
                "bnRMYW5kMREwDwYDVQQHEwhBZ2VudEFJTTEhMB8GA1UEChMYQWwgTXVzdGFrYWJh\n" +
                "bCBwdWJsaXNoaW5nMRcwFQYDVQQDDA5QVVRfVE9LRU5fSEVSRTCCASIwDQYJKoZI\n" +
                "hvcNAQEBBQADggEPADCCAQoCggEBAI1u9jPwuDRZmllf6sLqOqfh8eFT3ao+hOCt\n" +
                "Q/cr4GJr8FNFJDgZVPidKDASp5VpAQ7+xNCi6J6M7drt+v8Z48FZb+q5rtWwvWZ8\n" +
                "5krQTi2iNktVNTsuNwzjgjSePm4Nk8xwQWjuxrte+Cv9KyebVSAxZbS7c5qYz4n2\n" +
                "3u8O1aMq5EQEg/UnytRl5/WYgMvtAue+tJJdtEg6mpm+xHRJKCBDOc9SZhyosr/+\n" +
                "mbsna/GePuqHFfp5yKog7KdRTVgrJJqyVxtPFGl9kg2QGuzLiVzvWRK/6rxzlpRG\n" +
                "tOb0dqQx14zZjfkbfRpqFIsEfeElmWEV8QIWOEJwRObP1nnuagMCAwEAATANBgkq\n" +
                "hkiG9w0BAQUFAAOCAQEABiYgT6KRfgZlPaGDtcsyQQfyFaoRw5MVLKA6BKMNWrNs\n" +
                "tLUCsNv64TD1aPlEynF0XuqE90MOCtlswq+1iXbiRGD6/fNEbbpQpcSHNOVyZzrw\n" +
                "fXUwEpnQFaGIuxoKXD8jRy7XPEDH19K8GVG6S1LtNhfi+x5cv5MxL9hPjTbCY+DC\n" +
                "jAPDJZvYRBysJxfRgvtZ/H9BUqqj7DHHypvdj3oGQdgNM25W3pLCq0C27Ff2K3gS\n" +
                "VJm+mp3fgbk11FQ57WQS/XSD9bdgej/U/vol35WcCDjOks+2/Yy20Co60B7IhPVR\n" +
                "W2ZmFZ5Px4T1HKlBp4StjXPxAeixpO3aUznA4Dq+dA==\n" +
                "-----END CERTIFICATE-----\n";

    static {
        // Replacing the newline character to the system specific line separator, to make tests pass both on Linux and
        // Windows without user frustration
        PEM_ENCODED_SIGNED_CERT = PEM_ENCODED_SIGNED_CERT.replace("\n", System.lineSeparator());
    }

    CertificateHandler testedClass = null;

    public CertificateHandlerTest()
        throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, IOException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream inputStream = new ByteArrayInputStream(SIGNED_CERTIFICATE_BYTES);
        signedCertificate = (X509Certificate) certFactory.generateCertificate(inputStream);

        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setUp() {
        testedClass = CertificateHandler.getInstance();
    }

    @Test
    public void testValidPEMEncoding()
        throws IOException, NoSuchAlgorithmException {
        String encodedString = testedClass.pemEncode(signedCertificate);
        Assert.assertEquals("PEM encoding yielded an unexpected result", PEM_ENCODED_SIGNED_CERT, encodedString);
    }

    @Test(expected = PemGenerationException.class)
    public void testPEMEncodingWithInvalidInput()
        throws IOException, NoSuchAlgorithmException {
        testedClass.pemEncode(null);
    }

    public static KeyPair generateKeyPair()
        throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(KEYSIZE, random);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        return keyPair;
    }

    // Use this main to generate new byte[] / PEM encoded certificates if you need to replace them in the tests
    // Paste it in a different class, if the Eclipse doesn't recognize it

    // public static void main(String[] args)
    // throws GeneralSecurityException, IOException, Base64DecodingException {
    // Security.addProvider(new BouncyCastleProvider());
    //
    // String CERTIFICATE_SIGNING_REQUEST =
    // // "-----BEGIN CERTIFICATE REQUEST-----" +
    // "MIICtTCCAZ0CAQAwcDELMAkGA1UEBhMCSUwxEjAQBgNVBAgTCUFnZW50TGFuZDER" +
    // "MA8GA1UEBxMIQWdlbnRBSU0xITAfBgNVBAoTGEFsIE11c3Rha2FiYWwgcHVibGlz" +
    // "aGluZzEXMBUGA1UEAwwOUFVUX1RPS0VOX0hFUkUwggEiMA0GCSqGSIb3DQEBAQUA" +
    // "A4IBDwAwggEKAoIBAQCNbvYz8Lg0WZpZX+rC6jqn4fHhU92qPoTgrUP3K+Bia/BT" +
    // "RSQ4GVT4nSgwEqeVaQEO/sTQouiejO3a7fr/GePBWW/qua7VsL1mfOZK0E4tojZL" +
    // "VTU7LjcM44I0nj5uDZPMcEFo7sa7Xvgr/Ssnm1UgMWW0u3OamM+J9t7vDtWjKuRE" +
    // "BIP1J8rUZef1mIDL7QLnvrSSXbRIOpqZvsR0SSggQznPUmYcqLK//pm7J2vxnj7q" +
    // "hxX6eciqIOynUU1YKySaslcbTxRpfZINkBrsy4lc71kSv+q8c5aURrTm9HakMdeM" +
    // "2Y35G30aahSLBH3hJZlhFfECFjhCcETmz9Z57moDAgMBAAGgADANBgkqhkiG9w0B" +
    // "AQsFAAOCAQEABHJeAEBOxwH/2YyIJbM5rdxLy1/pkcimujsVTVa02fm3WyIb3zLK" +
    // "Nk4XEahrFXlbtY1ZbgAox2MoISrDNrN/RojdVC72tRKKgBl/q0zG22uMtByoTkbf" +
    // "l9KNPjBJGnERSDHpMa+BLTgScHQPlmatlMRNauJpcrknRtA5pd4fVTaPCO2JC7Lr" +
    // "GEgPTDZdtO6FPXk2mfnRolEqhvG8M8bUP9Ssik/YtWibZC2/9S656yOGyQz3m4QZ" +
    // "nScoKfrRVham47NZfhVmSYoC5KfrdxM/tFtr9xMZaZ2EBXvGF0yft+TKXD7h/FkC" +
    // "K1TzYAifpZ67aVGjq2waI6wV/4lIkfLILQ==";// +
    // // "-----END CERTIFICATE REQUEST-----";
    // com.sun.org.apache.xml.internal.security.Init.init();
    // byte[] csr = com.sun.org.apache.xml.internal.security.utils.Base64.decode(CERTIFICATE_SIGNING_REQUEST);
    // String asString = new String(csr);
    // System.out.println(asString);
    //
    // CertificateHandler certHandler = new CertificateHandler();
    // String issuer =
    // "CN=Test Issuer CN,OU=Test Issuer OU,O=Test Issuer O,L=Test Issuer L,ST=Test Issuer ST,C=Test Issuer C";
    // X500Name x500name = new X500Name(issuer);
    // KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
    // keyPairGen.initialize(2048, random);
    // KeyPair keyPair = keyPairGen.generateKeyPair();
    //
    // X509Certificate signedCert = certHandler.createClientCertificate(x500name, csr, keyPair.getPrivate());
    // byte[] encodedCert = signedCert.getEncoded();
    // asString = new String(encodedCert);
    // System.out.println(asString);
    //
    // String pemEncodedCertificate = certHandler.pemEncode(signedCert);
    // System.out.println(pemEncodedCertificate);
    // }
}
