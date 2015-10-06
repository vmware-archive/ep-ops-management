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

package org.hyperic.hq.bizapp.agent;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.StringUtil;
import org.hyperic.util.exec.Os;
import org.hyperic.util.security.SecurityUtil;

public class PlatformToken {

    private static String PLATFORM_TOKEN_FILE_NAME = "epops-token";
    private static String DEFAULT_LINUX_FILE_PATH = "/etc/epops/" + PLATFORM_TOKEN_FILE_NAME;
    private static String DEFAULT_WINDOWS_FILE_PATH = "\\VMware\\EP Ops Agent\\" + PLATFORM_TOKEN_FILE_NAME;
    private static String TOKEN_FILE_ENCODING = "UTF-8";
    private static String DEFAULT_PATH_VALUE = "*default*";
    private static double defaultOsVersion = 6.1; // Windows Server 2008 R2
    private static double windows2003Version = 5.2;
    private static Log LOGGER = LogFactory.getLog(PlatformToken.class);

    private String platformToken = null;
    private boolean isCreatedInCurrentRun = false;

    public PlatformToken(String userDefinedLinuxPath,
                         String userDefinedWindowsPath)
        throws TokenNotFoundException {
        // Read boot properties and load the file paths
        String tokenPath = calculateTokenPathBasedOnOS(userDefinedLinuxPath, userDefinedWindowsPath);
        loadTokenFromFile(tokenPath);
    }

    /**
     * Calculates the token file path, based on OS version
     * 
     * Runs on default values, unless the user has specified a custom token path If the user has specified a value of
     * *default*, this will also lead to default value behavior
     * 
     * Defaults are: Linux: /etc/agent-token Windows: %PROGRAMDATA%\VMware\EP Ops Agent\epops-token or
     * %ALLUSERSPROFILE%\VMware\EP Ops Agent\epops-token (for Windows Server 2003, or earlier)
     * 
     * For Windows: in case any of the environment variables does not exist, we fall back to a hard coded default:
     * PROGRAMDATA = %SystemDrive%\ProgramData ALLUSERSPROFILE = %SystemDrive%\Documents and Settings\All
     * Users\Application Data First trying to use the %SystemDrive% variable, and later on C:
     * 
     * This behavior, although not officially supported, should make the code less error prone.
     * 
     * In case of OS version parse error, we fall back to version 6.1 (Windows Server 2008 R2)
     * 
     * @param bootProperties
     * @return
     */
    private String calculateTokenPathBasedOnOS(String customLinuxPath,
                                               String customWindowsPath) {
        LOGGER.debug("Calculating platform token path");
        String calculatedTokenPath = null;
        String userDefinedPath = null;
        if (Os.isWindowsFamily()) {
            // Read platform path env variable according to Windows version
            userDefinedPath = customWindowsPath;
            if (!StringUtil.isNullOrEmpty(userDefinedPath) && !DEFAULT_PATH_VALUE.equals(userDefinedPath)) {
                return userDefinedPath;
            }

            // Calculate default path
            String systemDrive = System.getenv("SystemDrive");
            if (StringUtil.isNullOrEmpty(systemDrive)) {
                systemDrive = "C:"; // Fallback to default
            }
            LOGGER.debug("The system drive is " + systemDrive);
            String windowsDefaultFilePathPrefix = null;
            double osVersion = 0;
            try {
                osVersion = Double.parseDouble(Os.getVersion());
                LOGGER.debug("The OS version is " + osVersion);
            } catch (NumberFormatException ex) {
                LOGGER.error(String.format("Could not parse OS version number %s. Using default %s", Os.getVersion(),
                            defaultOsVersion));
                osVersion = defaultOsVersion;
            }

            if (osVersion <= windows2003Version) {
                windowsDefaultFilePathPrefix = System.getenv("ALLUSERSPROFILE");
                if (StringUtil.isNullOrEmpty(windowsDefaultFilePathPrefix)) {
                    windowsDefaultFilePathPrefix =
                                systemDrive + "\\Documents and Settings\\All Users\\Application Data";
                }
            } else {
                windowsDefaultFilePathPrefix = System.getenv("PROGRAMDATA");
                if (StringUtil.isNullOrEmpty(windowsDefaultFilePathPrefix)) {
                    windowsDefaultFilePathPrefix = systemDrive + "\\ProgramData";
                }
            }
            calculatedTokenPath = windowsDefaultFilePathPrefix + DEFAULT_WINDOWS_FILE_PATH;
        } else { // A non Windows based OS
            userDefinedPath = customLinuxPath;
            if (!StringUtil.isNullOrEmpty(userDefinedPath) && !DEFAULT_PATH_VALUE.equals(userDefinedPath)) {
                return userDefinedPath;
            }
            calculatedTokenPath = DEFAULT_LINUX_FILE_PATH;
        }
        LOGGER.debug("Calculated token path is " + calculatedTokenPath);
        return (calculatedTokenPath);

    }

    private void loadTokenFromFile(String filePath)
        throws TokenNotFoundException {
        LOGGER.info("Loading the platform token from " + filePath);
        if (StringUtil.isNullOrEmpty(filePath)) {
            throw new TokenNotFoundException("The token file path is undefined");
        }

        File platfromTokenFile = new File(filePath);
        try {
            if (!platfromTokenFile.exists()) {
                // Create the token file with a newly generated token
                String newPlatfromToken = SecurityUtil.generateRandomToken();
                FileUtils.writeStringToFile(platfromTokenFile, newPlatfromToken, TOKEN_FILE_ENCODING);
                LOGGER.info(String.format(
                            "The platform token file was not found in %s, because file does not exist. A new platform token %s has been created and stored in that location.",
                            filePath, newPlatfromToken));
                isCreatedInCurrentRun = true;
            }
            platformToken = FileUtils.readFileToString(platfromTokenFile, TOKEN_FILE_ENCODING).trim();
            SecurityUtil.validateToken(platformToken);
            LOGGER.info("Current platform token is " + platformToken);
        } catch (SecurityException tokenValidationException) {
            throw new TokenNotFoundException(
                        "The agent encountered an error when reading the token from the file at "
                                    + filePath + ": " + tokenValidationException.getMessage());
        } catch (IOException ex) {
            throw new TokenNotFoundException("The agent encountered an error when accessing the token file at "
                        + filePath + ": " + ex.getMessage());
        }
    }

    /**
     * @return the loaded platform token
     */
    public String getValue() {
        return platformToken;
    }

    public boolean wasTokenGeneratedByCurrentRun() {
        return isCreatedInCurrentRun;
    }

}
