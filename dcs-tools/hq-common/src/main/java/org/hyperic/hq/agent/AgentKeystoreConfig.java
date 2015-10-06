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

package org.hyperic.hq.agent;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.util.security.KeystoreConfig;

/**
 * This class will get the keystore property in agent's default property file (usually it's agent.properties) and create
 * a keystoreConfig for SSL communication (Should only be used for agent side code). Keystore credentials are managed by
 * KeystoreCredentialsManager, and fetched only when needed (lazy eval), in order to allow the keystore to initialize.
 */
public class AgentKeystoreConfig extends KeystoreConfig {
    private static final String DEFAULT_SSL_KEYSTORE_ALIAS = AgentConfig.DEFAULT_SSL_KEYSTORE_ALIAS;
    private static final String SSL_KEYSTORE_ALIAS = AgentConfig.SSL_KEYSTORE_ALIAS;
    private final Log log = LogFactory.getLog(AgentKeystoreConfig.class);
    private boolean acceptUnverifiedCertForPlugins;
    private char[] userDefaultPassword;

    public AgentKeystoreConfig() {
        AgentConfig cfg;
        final String propFile = System.getProperty(
                    AgentConfig.PROP_PROPFILE, AgentConfig.DEFAULT_PROPFILE);
        try {
            cfg = AgentConfig.newInstance(propFile);
            init(cfg.getBootProperties());
        } catch (IOException exc) {
            log.error("Error: " + exc, exc);
            return;
        } catch (AgentConfigException exc) {
            log.error("Agent properties error: " + exc.getMessage(), exc);
            return;
        }
    }

    public AgentKeystoreConfig(AgentConfig cfg) {
        this(cfg.getBootProperties());
    }

    public AgentKeystoreConfig(Properties bootProperties) {
        init(bootProperties);
    }

    private void init(Properties bootProperties) {
        this.userDefaultPassword = bootProperties.getProperty(AgentConfig.SSL_KEYSTORE_PASSWORD).toCharArray();
        super.setFilePath(bootProperties.getProperty(AgentConfig.SSL_KEYSTORE_PATH));
        super.setAlias(bootProperties.getProperty(SSL_KEYSTORE_ALIAS, DEFAULT_SSL_KEYSTORE_ALIAS));
        super.setClientCertificateAlias(bootProperties.getProperty(AgentConfig.SSL_CLIENT_CERTIFICATE_ALIAS,
                    AgentConfig.DEFAULT_SSL_CLIENT_CERTIFICATE_ALIAS));
        super.setServerCertificateAlias(bootProperties.getProperty(AgentConfig.SSL_SERVER_CERTIFICATE_ALIAS,
                    AgentConfig.DEFAULT_SSL_SERVER_CERTIFICATE_ALIAS));
        super.setHqDefault(AgentConfig.PROP_KEYSTORE_PATH[1].equals(getFilePath()));
        String prop = bootProperties.getProperty(AgentConfig.SSL_KEYSTORE_ACCEPT_UNVERIFIED_CERT);
        this.acceptUnverifiedCertForPlugins = Boolean.parseBoolean(prop);
        super.setKeyCN(AppdefEntityConstants.HQ_AGENT_PROTOTYPE_NAME);
    }

    @Override
    public String getFilePassword()
        throws IOException {
        return String.valueOf(getFilePasswordCharArray());
    }

    @Override
    public char[] getFilePasswordCharArray()
        throws IOException {
        return KeystoreCredentialsManager.getInstance().getKeystorePassword(new File(getFilePath()),
                    userDefaultPassword);
    }

    public boolean isAcceptUnverifiedCert() {
        return acceptUnverifiedCertForPlugins;
    }
}
