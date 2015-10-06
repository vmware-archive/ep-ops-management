/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.hyperic.util.exec;

import java.util.Locale;

/**
 * Condition that tests the OS type.
 * 
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public class Os {
    private static final String osName =
                System.getProperty("os.name").toLowerCase(Locale.US);
    private static final String osArch =
                System.getProperty("os.arch").toLowerCase(Locale.US);
    private static final String osVersion =
                System.getProperty("os.version").toLowerCase(Locale.US);
    private static final String pathSep = System.getProperty("path.separator");
    public static final String WINDOWS_FAMILY_STRING = "windows";

    /**
     * Determines if the OS on which Ant is executing matches the given OS family.
     * 
     * @param f The OS family type desired<br />
     *            Possible values:<br />
     *            <ul>
     *            <li>dos</li>
     *            <li>mac</li>
     *            <li>netware</li>
     *            <li>os/2</li>
     *            <li>unix</li>
     *            <li>windows</li>
     *            </ul>
     * @since 1.5
     */
    public static boolean isFamily(String family) {
        return isOs(family, null, null, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS name.
     * 
     * @since 1.7
     */
    public static boolean isName(String name) {
        return isOs(null, name, null, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS architecture.
     * 
     * @since 1.7
     */
    public static boolean isArch(String arch) {
        return isOs(null, null, arch, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS version.
     * 
     * @since 1.7
     */
    public static boolean isVersion(String version) {
        return isOs(null, null, null, version);
    }

    /**
     * For Windows, these are the common versions: Operating system | Version number Windows 8.1 | 6.3 Windows Server
     * 2012 R2 | 6.3 Windows 8 | 6.2 Windows Server 2012 | 6.2 Windows 7 | 6.1 Windows Server 2008 R2 | 6.1 Windows
     * Server 2008 | 6.0 Windows Vista | 6.0 Windows Server 2003 R2 | 5.2 Windows Server 2003 | 5.2 (os name is: Windows
     * 2003) Windows XP 64-Bit Edition | 5.2 Windows XP | 5.1 Windows 2000 | 5.0
     * 
     * @return the OS version number
     */
    public static String getVersion() {
        return osVersion;
    }

    public static boolean isWindowsFamily() {
        return isFamily(WINDOWS_FAMILY_STRING);
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS family, name, architecture and version
     * 
     * @param family The OS family
     * @param name The OS name
     * @param arch The OS architecture
     * @param version The OS version
     * 
     * @since 1.7
     */
    public static boolean isOs(String family,
                               String name,
                               String arch,
                               String version) {
        boolean retValue = false;

        if (family != null || name != null || arch != null
                    || version != null) {

            boolean isFamily = true;
            boolean isName = true;
            boolean isArch = true;
            boolean isVersion = true;

            if (family != null) {
                if (family.equals(WINDOWS_FAMILY_STRING)) {
                    isFamily = osName.indexOf(WINDOWS_FAMILY_STRING) > -1;
                } else if (family.equals("os/2")) {
                    isFamily = osName.indexOf("os/2") > -1;
                } else if (family.equals("netware")) {
                    isFamily = osName.indexOf("netware") > -1;
                } else if (family.equals("dos")) {
                    isFamily = pathSep.equals(";") && !isFamily("netware");
                } else if (family.equals("mac")) {
                    isFamily = osName.indexOf("mac") > -1;
                } else if (family.equals("unix")) {
                    isFamily = pathSep.equals(":")
                                && (!isFamily("mac") || osName.endsWith("x"));
                } else {
                    throw new RuntimeException(
                                "Don\'t know how to detect os family \""
                                            + family + "\"");
                }
            }
            if (name != null) {
                isName = name.equals(osName);
            }
            if (arch != null) {
                isArch = arch.equals(osArch);
            }
            if (version != null) {
                isVersion = version.equals(osVersion);
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }
}
