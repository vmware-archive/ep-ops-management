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

package org.hyperic.hq.product;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;

/**
 * Deployment of embedded plugin files. Plugins can embed scripts, jars, native libraries, etc. This class deploys such
 * files on the agent-side only to pdk/work/$type/$plugin/ Where type is "scripts", "lib", etc., plugin is the plugin
 * name. The pdk/$type directory must exist otherwise deployment is skipped.
 */
public class ClientPluginDeployer {

    private static final Log log =
                LogFactory.getLog(ClientPluginDeployer.class.getName());

    private final String plugin;
    private final String pdk;
    private static HashMap handlers = new HashMap();

    private static final String[] EX_DIRS = {
                "scripts", "lib"
    };

    private static final String[] DIRS = {
                "tmp"
    };

    static {
        addHandlers(EX_DIRS, true);
        addHandlers(DIRS, false);
    }

    public ClientPluginDeployer(String pdk,
                                String type) {
        this.pdk = pdk;
        this.plugin = type;
    }

    public static void addHandler(String dir,
                                  boolean isExecutable) {
        Handler handler = new Handler(dir);
        handlers.put(handler.getName(), handler);
        handler.setExecutable(isExecutable);
    }

    public static void addHandlers(String[] dirs,
                                   boolean isExecutable) {

        for (int i = 0; i < dirs.length; i++) {
            addHandler(dirs[i], isExecutable);
        }
    }

    public Handler getHandler(String name) {
        Handler handler = (Handler) handlers.get(name);
        if (handler != null) {
            return handler;
        }
        // e.g. "script" -> "scripts"
        return (Handler) handlers.get(name + 's');
    }

    public static File getSubDirectory(String root,
                                       String name,
                                       String plugin) {

        String subdir = "";
        if (!new File(root).getName().equals(AgentConfig.WORK_DIR)) {
            subdir += AgentConfig.WORK_DIR + File.separator;
        }
        subdir += name + File.separator + plugin;

        File dir = new File(root, subdir);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error("mkdir(" + dir + ") failed for plugin: " +
                            plugin);
                return null;
            }
            else {
                log.debug("mkdir(" + dir + ") succeeded for plugin: " +
                            plugin);
            }
        }

        return dir;
    }

    public static class Handler {
        private final String name;
        private boolean isExecutable;

        Handler(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean isExecutable() {
            return this.isExecutable;
        }

        public void setExecutable(boolean value) {
            this.isExecutable = value;
        }

        public File getSubDirectory(ClientPluginDeployer deployer) {
            return ClientPluginDeployer.getSubDirectory(deployer.pdk,
                        this.name,
                        deployer.plugin);
        }
    }

    public List unpackJar(String jar)
        throws IOException {

        ArrayList jars = new ArrayList();

        ZipFile zipfile = new ZipFile(jar);
        InputStream is = null;
        try {
            for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String name = entry.getName();

                if (entry.isDirectory()) {
                    continue;
                }

                int ix = name.indexOf('/');
                if (ix == -1) {
                    continue;
                }

                String prefix = name.substring(0, ix);
                name = name.substring(ix + 1);

                File file = getFile(prefix, name);
                if (file == null) {
                    continue;
                }

                if (name.endsWith(".jar")) {
                    jars.add(file.toString());
                }

                if (upToDate(entry, file)) {
                    continue;
                }

                is = zipfile.getInputStream(entry);
                write(is, file);
            }
        } finally {
            try {
                if (null != zipfile) {
                    zipfile.close();
                }
                if (null != is) {
                    is.close();
                }
            } catch (IOException exc) {
                // no op
            }
        }
        return jars;
    }

    public class PluginFile extends File {
        // shutup eclipse warning
        static final long serialVersionUID = 0xff;

        Handler handler;

        public PluginFile(String pathname) {
            super(pathname);
        }

        public PluginFile(File parent,
                          String child) {
            super(parent, child);
        }

        public PluginFile(String parent,
                          String child) {
            super(parent, child);
        }
    }

    public boolean isDeployableType(String type) {
        return getHandler(type) != null;
    }

    public File getFile(String type,
                        String file) {

        Handler handler = getHandler(type);

        if (handler == null) {
            return null;
        }

        File dir =
                    handler.getSubDirectory(this);

        if (dir == null) {
            log.debug("Unable to determine subdirectory to write: " +
                        file);
            return null;
        }

        PluginFile pluginFile = new PluginFile(dir, file);
        pluginFile.handler = handler;
        return pluginFile;
    }

    public boolean upToDate(long source,
                            long target) {
        return source < target;
    }

    public boolean upToDate(ZipEntry source,
                            File target) {
        boolean upToDate =
                    upToDate(source.getTime(),
                                target.lastModified());

        if (upToDate) {
            log.debug("Unchanged file: " + target);
        }

        return upToDate;
    }

    public boolean upToDate(File source,
                            File target) {
        boolean upToDate =
                    upToDate(source.lastModified(),
                                target.lastModified());

        if (upToDate) {
            log.debug("Unchanged file: " + target);
        }

        return upToDate;
    }

    public void write(String data,
                      File file)
        throws IOException {

        write(new ByteArrayInputStream(data.getBytes()), file);
    }

    public void write(InputStream is,
                      File file)
        throws IOException {

        if (file == null) {
            return;
        }

        boolean exists = file.exists();
        FileOutputStream os;

        try {
            os = new FileOutputStream(file);
            log.debug((exists ? "Updated" : "Created") +
                        " file: " + file);
        } catch (IOException e) {
            if (file.exists() && (file.length() > 0)) {
                // e.g. on win32, agent running w/ dll loaded
                // PluginDumper cannot overwrite file inuse.
                return;
            }
            else {
                String msg =
                            "Error writing " + file +
                                        ": " + e.getMessage();
                throw new IOException(msg);
            }
        }

        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        } finally {
            os.close();
        }

        if (file instanceof PluginFile) {
            Handler handler = ((PluginFile) file).handler;
            if (handler.isExecutable()) {
                chmodx(file);
            }
        }

    }

    public boolean chmod(File file,
                         String mode) {

        if (GenericPlugin.isWin32() || file == null) {
            return false;
        }

        if (!file.exists()) {
            log.error(String.format("Cannot chmod, file at %s doesn't exist.", file.getAbsolutePath()));
            return false;
        }

        if (!ChmodModeValidator.validateChmodMode(mode)) {
            log.error(String.format("Cannot chmod file at %s, invalid mode.", file.getAbsolutePath()));
            return false;
        }

        try {
            // XXX lame.
            String cmd = "chmod " + mode + " " + file;
            Process process =
                        Runtime.getRuntime().exec(cmd);

            try {
                return process.waitFor() == 0;
            } catch (InterruptedException e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public boolean chmodx(File file) {
        return chmod(file, "+x");
    }

    private static class ChmodModeValidator {

        private static final String SYMBOLIC_MODE_REGEX;
        private static final Pattern SYMBOLIC_MODE_PATTERN;
        private static final String ABSOLUTE_MODE_REGEX = "[0-7]{1,4}";
        private static final Pattern ABSOLUTE_MODE_PATTERN = Pattern.compile(ABSOLUTE_MODE_REGEX);

        static {
            SYMBOLIC_MODE_REGEX = createSymbolicModeRegex();
            SYMBOLIC_MODE_PATTERN = Pattern.compile(SYMBOLIC_MODE_REGEX);
        }

        private static boolean validateChmodMode(String mode) {
            if (validateAbsoluteMode(mode)) {
                return true;
            }
            return validateSymbolicMode(mode);
        }

        private static boolean validateAbsoluteMode(String mode) {
            Matcher m = ABSOLUTE_MODE_PATTERN.matcher(mode);
            return m.matches();
        }

        private static String createSymbolicModeRegex() {
            String perm = "rstwxXugo";
            String op = "\\+\\-\\=";
            String who = "augo";

            // action ::= op [perm ...]
            String action = String.format("%s\\s*%s", groupChars(op, false), groupChars(perm, true));

            // clause ::= [who ...] [action ...] action
            String clause = String.format("%s\\s*%s", groupChars(who, true), groupAndQuantifyRegex(action, false));

            // mode ::= clause [, clause ...]
            String modeRegex = String.format("%s%s", groupAndQuantifyRegex(clause, false),
                        groupAndQuantifyRegex(",\\s*" + clause, true));
            return modeRegex;
        }

        private static boolean validateSymbolicMode(String mode) {
            Matcher m = SYMBOLIC_MODE_PATTERN.matcher(mode);
            return m.matches();
        }

        private static String groupChars(String chars,
                                         boolean multipleTimes) {
            String wrappedChars = String.format("[%s]", chars);
            if (!multipleTimes) {
                return wrappedChars;
            }
            return String.format("%s*", wrappedChars);
        }

        private static String groupAndQuantifyRegex(String regex,
                                                    boolean allowZeroTimes) {
            String quanitifiedRegex = String.format("(%s)", regex);
            if (allowZeroTimes) {
                return String.format("%s*", quanitifiedRegex);
            }
            return String.format("%s+", quanitifiedRegex);
        }
    }
}
