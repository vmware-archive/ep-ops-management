/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.groovy;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ScriptLanguagePlugin;
import org.hyperic.util.PluginLoader;

public class GroovyLanguagePlugin
    extends ProductPlugin
    implements ScriptLanguagePlugin {

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);
        addScriptLanguage(this);
    }

    public String getExtension() {
        return "groovy";
    }

    //adding groovy-all-*.jar to the GroovyClassLoader
    //XXX dont like this but would like to keep groovy-all
    //out of the agent classloader
    private void adjustClassPath(GroovyClassLoader cl) {
        ClassLoader parent = getClass().getClassLoader();

        if (parent instanceof PluginLoader) {
            PluginLoader loader = (PluginLoader)parent;
            URL[] urls = loader.getURLs();

            //urls[0] == groovy-scripting-plugin.jar
            for (int i=1; i<urls.length; i++) {
                getLog().debug("Adding to classpath: " + urls[i]);
                cl.addURL(urls[i]);
            }
        }
    }

    public Class loadClass(ClassLoader loader,
                           Properties properties,
                           File file)
        throws PluginException {

        GroovyClassLoader cl = new GroovyClassLoader(loader);
        adjustClassPath(cl);

        try {
            // Also add the directory which contain the groovy script.  This
            // allows us to add .groovy scripts in scripts/ which are not
            // called directly via this function (i.e. only imported by
            // groovy scripts, plugin support)
            cl.addURL(file.getParentFile().toURL());
            return cl.parseClass(file);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
