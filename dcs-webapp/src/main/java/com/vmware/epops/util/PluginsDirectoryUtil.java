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

package com.vmware.epops.util;

public abstract class PluginsDirectoryUtil {

    public static final String PLUGIN_XML_SUFFIX = "-plugin.xml";
    public static final String PLUGIN_JAR_SUFFIX = "-plugin.jar";
    public static final String MD5_SUFFIX = ".md5";

    public static boolean isPluginFile(String name) {
        return name.endsWith(PLUGIN_JAR_SUFFIX) || name.endsWith(PLUGIN_XML_SUFFIX);
    }

    public static boolean isPluginMd5File(String name) {
        return name.endsWith(MD5_SUFFIX)
                    && PluginsDirectoryUtil.isPluginFile(name.substring(0, name.lastIndexOf(MD5_SUFFIX)));
    }

}
