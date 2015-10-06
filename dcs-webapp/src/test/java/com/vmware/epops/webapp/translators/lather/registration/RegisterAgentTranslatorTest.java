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

package com.vmware.epops.webapp.translators.lather.registration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Base64;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_result;
import org.hyperic.lather.LatherValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vmware.epops.command.AgentCommandData;
import com.vmware.epops.command.upstream.registration.RegisterAgentCommandResponse;
import com.vmware.epops.command.upstream.registration.RegisterCommandData;
import com.vmware.epops.util.security.CertificateHandler;
import com.vmware.epops.webapp.translators.lather.registration.RegisterAgentTranslator;

/**
 * Unit test for AgentRegistrationTranslator class
 * 
 * @author yyogev
 */
public class RegisterAgentTranslatorTest {

    private RegisterAgentTranslator tested;
    private RegisterAgentCommandResponse mockedReponse;
    private CertificateHandler mockedCertificateHandler;
    private static final String USERNAME = "some_username";
    private static final int CPU_COUNT = 7;
    private static final String VERSION = "7";
    private static final String IP = "10.0.0.7";
    private static final String PASSWORD = "some_password";

    private static final byte[] CERTIFICATE_SIGNING_REQUEST_BYTES =
    { 48, -126, 2, -75, 48, -126, 1, -99, 2, 1, 0, 48, 112, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 73, 76, 49,
                18, 48, 16, 6, 3, 85, 4, 8, 19, 9, 65, 103, 101, 110, 116, 76, 97, 110, 100, 49, 17, 48, 15, 6, 3, 85,
                4, 7,
                19, 8, 65, 103, 101, 110, 116, 65, 73, 77, 49, 33, 48, 31, 6, 3, 85, 4, 10, 19, 24, 65, 108, 32, 77,
                117, 115, 116, 97, 107, 97, 98, 97, 108, 32, 112, 117, 98, 108, 105, 115, 104, 105, 110, 103, 49, 23,
                48, 21, 6, 3, 85, 4, 3, 12, 14, 80, 85, 84, 95, 84, 79, 75, 69, 78, 95, 72, 69, 82, 69, 48, -126, 1,
                34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2,
                -126, 1, 1, 0, -115, 110, -10, 51, -16, -72, 52, 89, -102, 89, 95, -22, -62, -22, 58, -89, -31, -15,
                -31, 83, -35, -86, 62, -124, -32, -83, 67, -9, 43, -32, 98, 107, -16, 83, 69, 36, 56, 25, 84, -8, -99,
                40, 48, 18, -89, -107, 105, 1, 14, -2, -60, -48, -94, -24, -98, -116, -19, -38, -19, -6, -1, 25, -29,
                -63, 89, 111, -22, -71, -82, -43, -80, -67, 102, 124, -26, 74, -48, 78, 45, -94, 54, 75, 85, 53, 59,
                46, 55, 12, -29, -126, 52, -98, 62, 110, 13, -109, -52, 112, 65, 104, -18, -58, -69, 94, -8, 43, -3,
                43, 39, -101, 85, 32, 49, 101, -76, -69, 115, -102, -104, -49, -119, -10, -34, -17, 14, -43, -93, 42,
                -28, 68, 4, -125, -11, 39, -54, -44, 101, -25, -11, -104, -128, -53, -19, 2, -25, -66, -76, -110, 93,
                -76, 72, 58, -102, -103, -66, -60, 116, 73, 40, 32, 67, 57, -49, 82, 102, 28, -88, -78, -65, -2, -103,
                -69, 39, 107, -15, -98, 62, -22, -121, 21, -6, 121, -56, -86, 32, -20, -89, 81, 77, 88, 43, 36, -102,
                -78, 87, 27, 79, 20, 105, 125, -110, 13, -112, 26, -20, -53, -119, 92, -17, 89, 18, -65, -22, -68, 115,
                -106, -108, 70, -76, -26, -12, 118, -92, 49, -41, -116, -39, -115, -7, 27, 125, 26, 106, 20, -117, 4,
                125, -31, 37, -103, 97, 21, -15, 2, 22, 56, 66, 112, 68, -26, -49, -42, 121, -18, 106, 3, 2, 3, 1, 0,
                1, -96, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 3, -126, 1, 1, 0, 4, 114, 94, 0,
                64, 78, -57, 1, -1, -39, -116, -120, 37, -77, 57, -83, -36, 75, -53, 95, -23, -111, -56, -90, -70, 59,
                21, 77, 86, -76, -39, -7, -73, 91, 34, 27, -33, 50, -54, 54, 78, 23, 17, -88, 107, 21, 121, 91, -75,
                -115, 89, 110, 0, 40, -57, 99, 40, 33, 42, -61, 54, -77, 127, 70, -120, -35, 84, 46, -10, -75, 18,
                -118, -128, 25, 127, -85, 76, -58, -37, 107, -116, -76, 28, -88, 78, 70, -33, -105, -46, -115, 62, 48,
                73, 26, 113, 17, 72, 49, -23, 49, -81, -127, 45, 56, 18, 112, 116, 15, -106, 102, -83, -108, -60, 77,
                106, -30, 105, 114, -71, 39, 70, -48, 57, -91, -34, 31, 85, 54, -113, 8, -19, -119, 11, -78, -21, 24,
                72, 15, 76, 54, 93, -76, -18, -123, 61, 121, 54, -103, -7, -47, -94, 81, 42, -122, -15, -68, 51, -58,
                -44, 63, -44, -84, -118, 79, -40, -75, 104, -101, 100, 45, -65, -11, 46, -71, -21, 35, -122, -55, 12,
                -9, -101, -124, 25, -99, 39, 40, 41, -6, -47, 86, 22, -90, -29, -77, 89, 126, 21, 102, 73, -118, 2,
                -28, -89, -21, 119, 19, 63, -76, 91, 107, -9, 19, 25, 105, -99, -124, 5, 123, -58, 23, 76, -97, -73,
                -28, -54, 92, 62, -31, -4, 89, 2, 43, 84, -13, 96, 8, -97, -91, -98, -69, 105, 81, -93, -85, 108, 26,
                35, -84, 21, -1, -119, 72, -111, -14, -56, 45 };
    private static final byte[] SIGNED_CERTIFICATE_BYTES = { 48, -126, 3, -93, 48, -126, 2, -117, -96, 3, 2, 1, 2, 2,
                4, 67,
                -22, 26, 108, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 48, -127, -109, 49, 23, 48, 21,
                6, 3, 85, 4, 3, 12, 14, 84, 101, 115, 116, 32, 73, 115, 115, 117, 101, 114, 32, 67, 78, 49, 23, 48, 21,
                6, 3, 85, 4, 11, 12, 14, 84, 101, 115, 116, 32, 73, 115, 115, 117, 101, 114, 32, 79, 85, 49, 22, 48,
                20, 6, 3, 85, 4, 10, 12, 13, 84, 101, 115, 116, 32, 73, 115, 115, 117, 101, 114, 32, 79, 49, 22, 48,
                20, 6, 3, 85, 4, 7, 12, 13, 84, 101, 115, 116, 32, 73, 115, 115, 117, 101, 114, 32, 76, 49, 23, 48, 21,
                6, 3, 85, 4, 8, 12, 14, 84, 101, 115, 116, 32, 73, 115, 115, 117, 101, 114, 32, 83, 84, 49, 22, 48, 20,
                6, 3, 85, 4, 6, 19, 13, 84, 101, 115, 116, 32, 73, 115, 115, 117, 101, 114, 32, 67, 48, 30, 23, 13, 49,
                52, 48, 54, 49, 52, 50, 48, 51, 48, 49, 54, 90, 23, 13, 50, 52, 48, 52, 50, 50, 50, 48, 51, 48, 49, 54,
                90, 48, 112, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 73, 76, 49, 18, 48, 16, 6, 3, 85, 4, 8, 19, 9, 65,
                103, 101, 110, 116, 76, 97, 110, 100, 49, 17, 48, 15, 6, 3, 85, 4, 7, 19, 8, 65, 103, 101, 110, 116,
                65, 73, 77, 49, 33, 48, 31, 6, 3, 85, 4, 10, 19, 24, 65, 108, 32, 77, 117, 115, 116, 97, 107, 97, 98,
                97, 108, 32, 112, 117, 98, 108, 105, 115, 104, 105, 110, 103, 49, 23, 48, 21, 6, 3, 85, 4, 3, 12, 14,
                80, 85, 84, 95, 84, 79, 75, 69, 78, 95, 72, 69, 82, 69, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72,
                -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -115, 110, -10, 51,
                -16, -72, 52, 89, -102, 89, 95, -22, -62, -22, 58, -89, -31, -15, -31, 83, -35, -86, 62, -124, -32,
                -83, 67, -9, 43, -32, 98, 107, -16, 83, 69, 36, 56, 25, 84, -8, -99, 40, 48, 18, -89, -107, 105, 1, 14,
                -2, -60, -48, -94, -24, -98, -116, -19, -38, -19, -6, -1, 25, -29, -63, 89, 111, -22, -71, -82, -43,
                -80, -67, 102, 124, -26, 74, -48, 78, 45, -94, 54, 75, 85, 53, 59, 46, 55, 12, -29, -126, 52, -98, 62,
                110, 13, -109, -52, 112, 65, 104, -18, -58, -69, 94, -8, 43, -3, 43, 39, -101, 85, 32, 49, 101, -76,
                -69, 115, -102, -104, -49, -119, -10, -34, -17, 14, -43, -93, 42, -28, 68, 4, -125, -11, 39, -54, -44,
                101, -25, -11, -104, -128, -53, -19, 2, -25, -66, -76, -110, 93, -76, 72, 58, -102, -103, -66, -60,
                116, 73, 40, 32, 67, 57, -49, 82, 102, 28, -88, -78, -65, -2, -103, -69, 39, 107, -15, -98, 62, -22,
                -121, 21, -6, 121, -56, -86, 32, -20, -89, 81, 77, 88, 43, 36, -102, -78, 87, 27, 79, 20, 105, 125,
                -110, 13, -112, 26, -20, -53, -119, 92, -17, 89, 18, -65, -22, -68, 115, -106, -108, 70, -76, -26, -12,
                118, -92, 49, -41, -116, -39, -115, -7, 27, 125, 26, 106, 20, -117, 4, 125, -31, 37, -103, 97, 21, -15,
                2, 22, 56, 66, 112, 68, -26, -49, -42, 121, -18, 106, 3, 2, 3, 1, 0, 1, -93, 33, 48, 31, 48, 29, 6, 3,
                85, 29, 14, 4, 22, 4, 20, -67, -25, 72, -7, 123, -108, -15, 3, -72, 19, -23, 22, 44, -72, 78, -100,
                -128, 39, -25, -100, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 3, -126, 1, 1, 0, 106,
                -42, -33, -43, -20, -67, 34, -78, 59, -85, -126, -104, 52, 98, 112, -86, -83, 78, 4, -2, -108, -119,
                -16, -64, 31, -54, -2, -28, 119, 40, -43, 47, 49, 111, 112, -50, -42, 63, 57, -49, -73, -53, -28, -62,
                31, 117, 57, -76, -40, -64, -22, -79, -101, 15, -107, 45, -115, -8, 70, -123, 89, -19, -3, 52, -108,
                -106, 41, 82, -118, 112, 69, -119, -124, -39, 32, 35, -39, -49, 39, 31, 106, 28, 22, -14, 75, -43, -65,
                74, 83, -123, -68, 114, -35, 116, 122, -120, 40, 77, 84, 115, 117, 81, 79, 125, -72, 71, -112, 24, 105,
                -57, -109, -5, -89, 27, -60, -93, -54, 8, 109, 109, -114, 67, -59, -52, -105, -126, 110, -27, -79, -81,
                -9, 106, 46, -100, -118, 108, -69, -19, -128, -36, 40, 90, -118, 54, -31, 57, -50, 2, 31, -9, -32,
                -115, 42, -101, 67, -59, 69, -33, 70, -76, -18, -66, -112, -76, -35, -121, -66, 21, -8, 77, -10, -123,
                41, -102, -12, -31, -12, -105, -14, 2, 36, 118, -3, -57, -15, -102, -62, 25, -107, 80, 127, 39, -15,
                111, 112, -25, -61, 126, 72, 74, -55, -29, -58, -122, 28, -113, 114, -114, -120, 104, 16, -52, -10,
                -63, -121, 98, -61, -89, -21, -94, -93, 71, 103, -93, 105, 66, 51, -2, -71, -27, 6, -97, -74, 70, 80,
                49, -126, 5, 122, -46, -50, 101, -50, 74, 114, -31, 125, 110, 10, 117, 112, 79, 83, 94, -30, -32 };

    private static final String SIGNED_CERTIFICATE_PEM_ENCODED = "-----BEGIN CERTIFICATE-----"
                + "MIIDezCCAmMCBAKvoCgwDQYJKoZIhvcNAQEFBQAwgZMxFzAVBgNVBAMMDlRlc3Qg"
                + "SXNzdWVyIENOMRcwFQYDVQQLDA5UZXN0IElzc3VlciBPVTEWMBQGA1UECgwNVGVz"
                + "dCBJc3N1ZXIgTzEWMBQGA1UEBwwNVGVzdCBJc3N1ZXIgTDEXMBUGA1UECAwOVGVz"
                + "dCBJc3N1ZXIgU1QxFjAUBgNVBAYTDVRlc3QgSXNzdWVyIEMwHhcNMTQwNjE1MTIw"
                + "OTA2WhcNMjQwNDIzMTIwOTA2WjBwMQswCQYDVQQGEwJJTDESMBAGA1UECBMJQWdl"
                + "bnRMYW5kMREwDwYDVQQHEwhBZ2VudEFJTTEhMB8GA1UEChMYQWwgTXVzdGFrYWJh"
                + "bCBwdWJsaXNoaW5nMRcwFQYDVQQDDA5QVVRfVE9LRU5fSEVSRTCCASIwDQYJKoZI"
                + "hvcNAQEBBQADggEPADCCAQoCggEBAI1u9jPwuDRZmllf6sLqOqfh8eFT3ao+hOCt"
                + "Q/cr4GJr8FNFJDgZVPidKDASp5VpAQ7+xNCi6J6M7drt+v8Z48FZb+q5rtWwvWZ8"
                + "5krQTi2iNktVNTsuNwzjgjSePm4Nk8xwQWjuxrte+Cv9KyebVSAxZbS7c5qYz4n2"
                + "3u8O1aMq5EQEg/UnytRl5/WYgMvtAue+tJJdtEg6mpm+xHRJKCBDOc9SZhyosr/+"
                + "mbsna/GePuqHFfp5yKog7KdRTVgrJJqyVxtPFGl9kg2QGuzLiVzvWRK/6rxzlpRG"
                + "tOb0dqQx14zZjfkbfRpqFIsEfeElmWEV8QIWOEJwRObP1nnuagMCAwEAATANBgkq"
                + "hkiG9w0BAQUFAAOCAQEAIvOBqpwfPTB3M/4ikXIcZ2wN32MpF98jZkpM0oAylZ3O"
                + "DUi1UUxW9yj9cuyYYdD73fMWoTp7J9FJ0c0qfWKgwQILwNCSiEsUVecVm+F5qCWa"
                + "aqtjFkC6/PCaEsbwQf49YMGP8flrbOHHvVinnJ6AtXgqvgvASpIEfcVvtCAQzGPj"
                + "Xd8Fb2Ql8nmRwWypD5Ll0+7BgGTcjS9rCzCbHnP71skQYotwAZCKdO/zekTzG+hh"
                + "yLCms41cPpHvRFQ4pQ0MehydahoVbWaRgLzyZ5vLCTJ+fmdfIZds7I963VmUJoaY"
                + "wv8zYlWpRlqp05s7YgViIfJihqS2OShV2yKfSykYKQ=="
                + "-----END CERTIFICATE-----";
    private final X509Certificate signedCertificate;

    public RegisterAgentTranslatorTest()
        throws CertificateException, NoSuchAlgorithmException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(SIGNED_CERTIFICATE_BYTES);
        signedCertificate = (X509Certificate) certFactory.generateCertificate(in);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
        throws Exception {
        this.tested = new RegisterAgentTranslator();
        this.mockedReponse = Mockito.mock(RegisterAgentCommandResponse.class);
    }

    /**
     * Test method for {@link RegisterAgentTranslator#translateRequest(LatherValue)}
     * 
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testTranslateRequest()
        throws NoSuchAlgorithmException {

        // == Not null parameter ==
        RegisterAgent_args args = getFilledArgs();

        AgentCommandData translated = tested.translateRequest(args);

        Assert.assertTrue("Translated Request doesn't match expected",
                    translated instanceof RegisterCommandData);

        RegisterCommandData regCommandData = (RegisterCommandData) translated;

        // Check values
        Assert.assertEquals("IP wasn't as expected", IP, regCommandData.getAgentIp());
        Assert.assertEquals("Password wasn't as expected", PASSWORD, regCommandData.getPassword());
        Assert.assertEquals("Username wasn't as expected", USERNAME, regCommandData.getUserName());
        Assert.assertEquals("CPU count wasn't as expected", CPU_COUNT, regCommandData.getCpuCount());
        Assert.assertEquals("Version wasn't as expected", VERSION, regCommandData.getVersion());
        Assert.assertTrue("Certificate wasn't as the one that was sent from the agent",
                    Arrays.equals(CERTIFICATE_SIGNING_REQUEST_BYTES,
                                Base64.decode(regCommandData.getCertificateRequest())));
    }

    /**
     * Test method for {@link RegisterAgentTranslator#translateRequest(LatherValue)} for null parameter
     */
    @Test
    public void testTranslateRequestNull() {
        // == Null parameter ==
        AgentCommandData translated = tested.translateRequest(null);
        Assert.assertNull("Expected null", translated);
    }

    /**
     * Test method for {@link AgentRegistrationTranslator#translateResponse(...)}
     * 
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @Test
    public void testTranslateResponse()
        throws IOException, GeneralSecurityException {
        // == Test normal response values ==
        String errorString = "error_string";
        Mockito.when(mockedReponse.getErrorString()).thenReturn(errorString);
        Mockito.when(mockedReponse.getCertificate()).thenReturn(SIGNED_CERTIFICATE_PEM_ENCODED);

        LatherValue translated = tested.translateResponse(mockedReponse);

        Assert.assertTrue("Translated response doesn't match expected", translated instanceof RegisterAgent_result);
        RegisterAgent_result regAgentresult = (RegisterAgent_result) translated;
        Assert.assertEquals("Returned certificate in response wasn't as expected", SIGNED_CERTIFICATE_PEM_ENCODED,
                    regAgentresult.getCertificate());
        Assert.assertEquals("Returned error message wasn't as expected", errorString,
                    regAgentresult.getErrorMessage());
    }

    /**
     * Test method for {@link AgentRegistrationTranslator#translateResponse(...)} with empty string agent token
     * 
     * @throws IOException
     */
    @Test(expected = RuntimeException.class)
    public void testTranslatePemEncodingThrowsException()
        throws IOException {

        // == Test response translation with the PEM encoder throwing an IOException ==
        Mockito.when(mockedCertificateHandler.pemEncode(signedCertificate)).thenThrow(
                    new IOException("Things have just got real"));
        tested.translateResponse(mockedReponse);
    }

    /**
     * Test method for {@link AgentRegistrationTranslator#translateResponse(...)} for null parameter
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTranslateResponseNull() {

        // == Null parameter ==
        tested.translateResponse(null); // Throws exception
    }

    @Test
    public void testTranslateResponseNoErrorNoCertificate()
        throws IOException, GeneralSecurityException {
        // == Test normal response values ==

        try {
            tested.translateResponse(mockedReponse);
        } catch (RuntimeException e) {
            Assert.assertEquals("Wrong exception message", "Invalid server response", e.getMessage());
            return;
        }
        Assert.fail("Expected exception when no certificate nor error message given");
    }

    @Test
    public void testTranslateResponseNoCertificateWithError()
        throws IOException, GeneralSecurityException {
        // == Test normal response values ==
        String errorString = "error_string";
        Mockito.when(mockedReponse.getErrorString()).thenReturn(errorString);

        LatherValue translated = tested.translateResponse(mockedReponse);

        Assert.assertTrue("Translated response doesn't match expected", translated instanceof RegisterAgent_result);
        RegisterAgent_result regAgentresult = (RegisterAgent_result) translated;
        Assert.assertEquals("Returned error message wasn't as expected", errorString,
                    regAgentresult.getErrorMessage());
    }

    private RegisterAgent_args getFilledArgs()
        throws NoSuchAlgorithmException {
        RegisterAgent_args args = new RegisterAgent_args();
        args.setAgentIP(IP);
        args.setUser(USERNAME);
        args.setPword(PASSWORD);
        args.setCpuCount(CPU_COUNT);
        args.setVersion(VERSION);
        args.setCertificateRequest(CERTIFICATE_SIGNING_REQUEST_BYTES);

        return args;
    }
}
