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

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Security;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CertificateHandler {

    private static final CertificateHandler instance = new CertificateHandler();
    private static final int VALID_TOKEN_LENGTH = 100;
    private static final String INVALID_TOKEN_LENGTH_ERROR =
                "Invalid token length found when extracting token from certificate. Continuing with null token. Invalid length was: {}";
    private static final String INVALID_TOKEN_PATTERN_ERROR =
                "Invalid token pattern found when extracting token from certificate. Continuing with null token.";
    private static final int VALID_SERIAL_LENGTH = 40; // 20 octets = 40 characters
    private static final String INVALID_SERIAL_PATTERN_ERROR =
                "Invalid serial number pattern found when extracting serial from certificate. Continuing with an empty serial.";
    private static final String INVALID_SERIAL_LENGTH_ERROR =
                "Invalid serial number length found when extracting serial from certificate. Continuing with an empty serial. Invalid length was: {}";
    private final static Logger logger = LoggerFactory.getLogger(CertificateHandler.class);
    // Either an HQ Token or a UUID (digits,letters and hyphens allowed)
    private final static Pattern tokenPatternInDn = Pattern.compile("CN=([-\\da-fA-F]+)$");
    private final static Pattern serialNumberPattern = Pattern.compile("^\\p{XDigit}{1," + VALID_SERIAL_LENGTH + "}$",
                Pattern.CASE_INSENSITIVE);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private CertificateHandler() {
    }

    public static CertificateHandler getInstance() {
        return instance;
    }

    public String extractAgentTokenFromRequest(HttpServletRequest request) {
        String token = "";

        String clientCertificateDn = (String) request.getAttribute("SSL_CLIENT_S_DN");
        if (null != clientCertificateDn) {
            Matcher matcher = tokenPatternInDn.matcher(clientCertificateDn);
            if (!matcher.find()) {
                logger.error(INVALID_TOKEN_PATTERN_ERROR);
                return null;
            }
            token = matcher.group(1);
        }
        // Protect from non expected values
        int tokenLength = token.length();
        if (tokenLength <= 0 || tokenLength > VALID_TOKEN_LENGTH) {
            logger.error(INVALID_TOKEN_LENGTH_ERROR, tokenLength);
            return null;
        }
        return token;
    }

    public String extractCertificateSerialNumberFromRequest(HttpServletRequest request) {
        String clientCertificateSerialNumber = (String) request.getAttribute("SSL_CLIENT_M_SERIAL");
        if (null != clientCertificateSerialNumber) {
            int serialNumberLength = clientCertificateSerialNumber.length();
            if (serialNumberLength <= 0 || serialNumberLength > VALID_SERIAL_LENGTH) {
                logger.error(INVALID_SERIAL_LENGTH_ERROR, serialNumberLength);
                return "";
            }
            Matcher matcher = serialNumberPattern.matcher(clientCertificateSerialNumber);
            if (!matcher.find()) {
                logger.error(INVALID_SERIAL_PATTERN_ERROR);
                return "";
            }
            String normalizedSerialNumber = trimLeadingZeros(clientCertificateSerialNumber);
            return normalizedSerialNumber;
        }
        logger.error(INVALID_SERIAL_PATTERN_ERROR);
        return "";
    }

    /**
     * Sometimes apache httpd prepends the serial number with leading zeros. We want to trim them in order to match the
     * saved serial.
     * 
     * @param clientCertificateSerialNumber
     * @return
     */
    private String trimLeadingZeros(String clientCertificateSerialNumber) {
        String normalizedSerialNumber = "";
        try {
            BigInteger serialBigInteger = new BigInteger(clientCertificateSerialNumber, 16);
            normalizedSerialNumber = serialBigInteger.toString(16);
        } catch (NumberFormatException e) {
            logger.error(INVALID_SERIAL_PATTERN_ERROR);
        }
        return normalizedSerialNumber;
    }

    /**
     * Utility for converting a security artifact to a string
     * 
     * @param obj Object The security artifact: key, certificate, etc.
     * @return String
     * @throws IOException
     * @throws Exception
     */
    public String pemEncode(Object obj)
        throws IOException {
        StringWriter sw = new StringWriter();

        try (PEMWriter pw = new PEMWriter(sw)) {
            pw.writeObject(obj);
            pw.flush();
        }
        return sw.toString();
    }
}
