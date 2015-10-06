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
package org.hyperic.hq.plugin.postgresql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class PostgreSQLServerDetector extends ServerDetector implements AutoServerDetector {
    private final Log log = LogFactory.getLog(PostgreSQLServerDetector.class);
    private static final String PTQL_QUERY = "State.Name.re=post(master|gres),State.Name.Pne=$1,Args.0.re=.*post(master|gres)(.exe)?$";
    protected static final String DB_QUERY = "SELECT datname FROM pg_database WHERE datistemplate IS FALSE AND datallowconn IS TRUE";

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);
        log.debug("[getServerProcessList] pids.length=" + pids.length);

        for (int i = 0; i < pids.length; i++) {
            String exe = getProcExe(pids[i]);
            List args = Arrays.asList(getProcArgs(pids[i]));
            log.debug("[getServerProcessList] pid=" + pids[i] + " exec=" + exe + " args=" + args);
            if (exe != null) {
                String version = getVersion(exe);
                String pgData = getArgument("-D", args);
                try {
                    pgData = new File(pgData).getCanonicalPath();
                } catch (IOException ex) {
                    log.debug(ex, ex);
                }

                ServerResource server = createServerResource(exe);
                server.setMeasurementConfig();
                server.setIdentifier(server.getIdentifier() + "$" + pgData);
                ConfigResponse cprop = new ConfigResponse();
                cprop.setValue("version", version);
                setCustomProperties(server, cprop);
                ConfigResponse productConfig = prepareConfig(pgData, args);
                setProductConfig(server, productConfig);
                String basename = getTypeInfo().getName();
                String platformName = platformConfig.getValue(ProductPlugin.PROP_PLATFORM_FQDN, getPlatformName());
                server.setName(prepareName(basename + HQConstants.RESOURCE_NAME_DELIM + platformName, server.getProductConfig(), null));
                servers.add(server);
            }
        }
        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        log.debug("[discoverServices] config=" + serverConfig);
        ArrayList services = new ArrayList();
        String user = serverConfig.getValue(PostgreSQL.PROP_USER);
        String pass = serverConfig.getValue(PostgreSQL.PROP_PASS);
        String url = PostgreSQL.prepareUrl(serverConfig.toProperties(), null);

        try {
            Class.forName(ResourceMeasurement.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Unable to load JDBC Driver: " + e.getMessage());
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        List<String> dataBases = new ArrayList<String>();
        try {
            conn = DriverManager.getConnection(url, user, pass);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(DB_QUERY);
            while (rs != null && rs.next()) {
                dataBases.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw PostgreSQL.getSecuredException(e);
        } finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }

        log.debug("[discoverServices] databases: " + dataBases);
        for (String dataBase : dataBases) {
            ServiceResource db = new ServiceResource();
            db.setType(this, PostgreSQL.PROP_DATABASE);

            ConfigResponse dbPC = new ConfigResponse();
            dbPC.setValue(PostgreSQL.PROP_DB, dataBase);

            db.setProductConfig(dbPC);
            db.setMeasurementConfig();

            db.setServiceName(prepareName(PostgreSQL.DB_NAME, serverConfig, dbPC));

            services.add(db);
        }
        return services;
    }

    private String prepareName(String pattern, ConfigResponse serverConf, ConfigResponse serviceConf) {
        List<ConfigResponse> props = new ArrayList<ConfigResponse>();
        String res = pattern;
        props.add(serverConf);
        if (serviceConf != null) {
            props.add(serviceConf);
        }

        for (ConfigResponse cfg : props) {
            for (String key : cfg.getKeys()) {
                String val = cfg.getValue(key);
                if (val == null) {
                    val = "";
                }
                res = res.replace("${" + key + "}", val);
            }
        }
        return res.trim();
    }

    private String getVersion(String exec) {
        String command[] = {exec, "--version"};
        log.debug("[getVersionString] command= '" + Arrays.asList(command) + "'");
        String version = "";
        try {
            Process cmd = Runtime.getRuntime().exec(command);
            cmd.getOutputStream().close();
            cmd.waitFor();
            String out = inputStreamAsString(cmd.getInputStream());
            String err = inputStreamAsString(cmd.getErrorStream());
            if (log.isDebugEnabled()) {
                if (cmd.exitValue() != 0) {
                    log.error("[getVersionString] exit=" + cmd.exitValue());
                    log.error("[getVersionString] out=" + out);
                    log.error("[getVersionString] err=" + err);
                } else {
                    log.debug("[getVersionString] out=" + out);
                }
            }
            version = out;
        } catch (InterruptedException ex) {
            log.debug("[getVersionString] Error:" + ex.getMessage(), ex);
        } catch (IOException ex) {
            log.debug("[getVersionString] Error:" + ex.getMessage(), ex);
        }
        return version;
    }

    private String inputStreamAsString(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            br.close();
        }
        return sb.toString().trim();
    }

    private String getArgument(String opt, List<String> args) {
        String res = "";
        int i = args.indexOf(opt);
        if (i > -1) {
            res = args.get(i + 1);
        }
        return res.trim();
    }

    private String getConfiguration(String regex, String config) {
        String res = "";
        Matcher m = Pattern.compile(regex, Pattern.MULTILINE + Pattern.CASE_INSENSITIVE).matcher(config);
        if (m.find()) {
            res = m.group(1).trim();
            res = res.replaceAll("=", "");
            res = res.replaceAll("'", "");
            res = res.replaceAll("\"", "");
        }
        return res.trim();
    }

    private String loadConfiguration(String pgData) {
        String configuration = "";
        File configFile = new File(pgData, "postgresql.conf");
        if (configFile.exists() && configFile.canRead()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(configFile);
                configuration = inputStreamAsString(in);
            } catch (IOException ex) {
                log.error("Error reading file '" + configFile + "': " + ex.getMessage(), ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        log.error("Error closing file '" + configFile + "'");
                    }
                }
            }
        }
        return configuration;
    }

    private ConfigResponse prepareConfig(String pgData, List<String> args) {
        ConfigResponse configResponse = new ConfigResponse();
        configResponse.setValue(PostgreSQL.PROP_DATA, pgData);
        String configuration = loadConfiguration(pgData);

        String addr = getConfiguration("^ *listen_addresses([^#]*)$", configuration);
        if (addr.length() > 0) {
            if (addr.equals("*")) {
                addr = "localhost";
            }
            configResponse.setValue(PostgreSQL.PROP_HOST, addr);
        } else {
            addr = getArgument("-h", args);
            if (addr.length() > 0) {
                configResponse.setValue(PostgreSQL.PROP_HOST, addr);
            }
        }

        String port = getConfiguration("^ *port([^#]*)$", configuration);
        if (port.length() > 0) {
            configResponse.setValue(PostgreSQL.PROP_PORT, port);
        } else {
            port = getArgument("-p", args);
            if (port.length() > 0) {
                configResponse.setValue(PostgreSQL.PROP_PORT, port);
            }
        }
        return configResponse;
    }

    private boolean isValidName(String name, boolean all, boolean off, Pattern reg) {
        boolean res;
        if (all) {
            res = true;
        } else if (off) {
            res = false;
        } else {
            res = reg.matcher(name).matches();
        }
        return res;
    }
}
