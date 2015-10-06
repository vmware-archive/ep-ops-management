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

package org.hyperic.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PluginLoader extends URLClassLoader {

    private static Log _log =
                LogFactory.getLog(PluginLoader.class.getName());
    // XXX WAS 6.1 seems to have an issue with %20
    private static final boolean ESCAPE_SPACES =
                !"false".equals(System.getProperty("PluginLoader.ESCAPE_SPACES"));
    private final Map addedURLs = new HashMap();
    private String pluginClassName = null;
    private String pluginName;
    private final ThreadLocal<ClassLoader> previousClassLoader = new ThreadLocal<ClassLoader>();

    private static String toFileURL(String file) {
        if (ESCAPE_SPACES) {
            file = StringUtil.replace(file, " ", "%20");
        }
        return "file:" + file;
    }

    private static URL toJarURL(String file)
        throws MalformedURLException {

        return new URL("jar", "", toFileURL(file) + "!/");
    }

    private static URL toURL(String file)
        throws MalformedURLException {

        return new URL(toFileURL(file));
    }

    /*
     * if classname is a jar, try to configure the plugin using jar
     * attributes
     */
    public static String getPluginMainClass(URL url)
        throws Exception {

        JarURLConnection jarConn = null;
        try {
            jarConn = (JarURLConnection) url.openConnection();
            Attributes attrs = jarConn.getMainAttributes();

            String pluginName = attrs.getValue(Attributes.Name.MAIN_CLASS);
            return pluginName;
        } finally {
            if (null != jarConn) {
                try {
                    jarConn.getJarFile().close(); // Written in tears and blood
                } catch (IOException exc) {
                    _log.error("Could not close file handler on " + url.toString()
                                + ". This may fail solution/product upgrade on Windows. ", exc);
                }
            }
        }
    }

    /*
     * allow each plugin to have their own classpath.
     */
    public static PluginLoader create(String pluginName,
                                      ClassLoader parent)
        throws PluginLoaderException {

        String pluginClassName = null;
        URL[] classpath;

        if (pluginName.endsWith(".jar")) {
            ArrayList urls = new ArrayList();
            try {
                URL jarUrl = toJarURL(pluginName);
                urls.add(jarUrl); // note-to-self

                pluginClassName = getPluginMainClass(jarUrl);
            } catch (Exception e) {
                String msg =
                            "failed to configure plugin jar=" + pluginName;
                throw new PluginLoaderException(msg, e);
            }

            classpath = (URL[]) urls.toArray(new URL[0]);
        }
        else {
            classpath = new URL[0];
        }

        PluginLoader loader = new PluginLoader(classpath, parent, pluginClassName);
        loader.pluginName = new File(pluginName).getName();
        return loader;
    }

    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static void resetClassLoader(Object obj) {
        ClassLoader cl = obj.getClass().getClassLoader();
        // XXX: should probably be dealt with by the caller
        if (cl instanceof PluginLoader) {
            PluginLoader pl = (PluginLoader) cl;

            setClassLoader(pl.previousClassLoader.get());

            pl.previousClassLoader.set(null);
        }
    }

    public static boolean setClassLoader(Object obj) {
        ClassLoader current = getClassLoader();
        ClassLoader cl = obj.getClass().getClassLoader();

        if (cl == current || !(cl instanceof PluginLoader)) {
            return false; // already set or not a PluginLoader
        }

        PluginLoader pl = (PluginLoader) cl;

        pl.previousClassLoader.set(current);

        setClassLoader(pl);

        return true;
    }

    public static void setClassLoader(ClassLoader loader) {
        Thread.currentThread().setContextClassLoader(loader);
    }

    public Class loadPlugin()
        throws ClassNotFoundException, PluginLoaderException {
        if (pluginClassName == null) {
            String msg =
                        "No Main-Class attribute found in MANIFEST";
            throw new PluginLoaderException(msg);
        }

        return loadClass(pluginClassName);
    }

    public Class loadPlugin(String name,
                            byte[] bytecode,
                            int len) {
        return defineClass(name, bytecode, 0, len);
    }

    public void addURL(String url)
        throws PluginLoaderException {

        addURL(new File(url));
    }

    private static class ClassPathFilter implements FilenameFilter {
        private final String start;
        private final String end;

        private ClassPathFilter(String start,
                                String end) {
            this.start = start;
            this.end = end;
        }

        public boolean accept(File file,
                              String name) {
            return name.startsWith(this.start) && name.endsWith(this.end);
        }
    }

    private void logAddPath(File path,
                            boolean isExpand) {
        if (_log.isDebugEnabled()) {
            if (isExpand) {
                String url = path.toString();
                if (addedURLs.get(url) == Boolean.TRUE) {
                    return; // only print once
                }
                _log.debug(this.pluginName + " expanding " + path + "...");
                addedURLs.put(url, Boolean.TRUE);
            }
            else {
                if (path.canRead()) {
                    _log.debug(this.pluginName + " += " + path);
                }
                else {
                    _log.debug(this.pluginName + " -= " + path +
                                ": Permission denied");
                }
            }
        }
    }

    public static String[] expand(File file) {
        String name = file.getName();
        int ix = name.indexOf("*"); // good enough for version matching
        if (ix == -1) {
            return null;
        }
        File dir = file.getParentFile();
        if (!dir.isDirectory()) {
            return null;
        }

        // support: "repository/geronimo/jars/geronimo-management-*.jar"
        String start = name.substring(0, ix);
        String end = name.substring(ix + 1);

        return dir.list(new ClassPathFilter(start, end));
    }

    public void addURL(File file)
        throws PluginLoaderException {

        boolean isExpand = false;
        String[] jars = null;

        if (!file.exists()) {
            jars = expand(file);
            if (jars == null) {
                return;
            }
            isExpand = true;
            logAddPath(file, true);
            file = file.getParentFile();
        }

        if (isExpand || file.isDirectory()) {
            if (!isExpand) {
                boolean addedDir = addedURLs.get(file) == Boolean.TRUE;
                logAddPath(file, true);
                if (!addedDir) {
                    addedURLs.put(file, Boolean.TRUE);
                    // add the directory itself
                    // must end with '/' (see URLClassLoader)
                    try {
                        super.addURL(toURL(file.toString() + "/"));
                    } catch (Exception e) {
                        throw new PluginLoaderException(e.getMessage());
                    }
                }
                jars = file.list();
            }

            if (jars == null) {
                return;
            }

            for (int j = 0; j < jars.length; j++) {
                addURL(new File(file, jars[j]));
            }

            return;
        }

        if (!file.exists()) {
            return;
        }
        String url = file.toString();
        if (addedURLs.get(url) == Boolean.TRUE) {
            return;
        }
        addedURLs.put(url, Boolean.TRUE);

        try {
            logAddPath(file, false);
            addURL(toURL(url));
        } catch (Exception e) {
            throw new PluginLoaderException(e.getMessage());
        }
    }

    public void addURLs(String[] urls)
        throws PluginLoaderException {

        for (int i = 0; i < urls.length; i++) {
            addURL(urls[i]);
        }
    }

    public void addURLs(List urls)
        throws PluginLoaderException {

        for (int i = 0; i < urls.size(); i++) {
            addURL((String) urls.get(i));
        }
    }

    private PluginLoader(URL[] urls,
                         ClassLoader parent,
                         String name)
    {
        super(urls, parent);
        pluginClassName = name;
        previousClassLoader.set(getClassLoader());
    }

    @Override
    public String toString() {
        URL[] urls = getURLs();
        String s = super.toString() + "/" + this.pluginName + "=[";

        for (int i = 0; i < urls.length; i++) {
            s += urls[i].getFile();
            if (i < urls.length - 1) {
                s += ", ";
            }
        }

        s += "]";
        return s;
    }

    // hook for plugins to avoid having to set
    // LD_LIBRARY_PATH, SHLIB_PATH, PATH, etc.
    // in order to find third-party native libs.
    @Override
    protected String findLibrary(String libname) {
        String lib =
                    System.getProperty("net.covalent.lib." + libname);

        if (lib != null) {
            return lib;
        }

        return super.findLibrary(libname);
    }
}
