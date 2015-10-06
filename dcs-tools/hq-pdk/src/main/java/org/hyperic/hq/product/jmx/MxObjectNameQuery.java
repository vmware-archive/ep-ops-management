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

package org.hyperic.hq.product.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPluginManager;

class MxObjectNameQuery extends MxServiceQuery {

    private String type;
    private Map props;
    private ArrayList names;

    MxObjectNameQuery() {
    }

    void setType(String type) {
        this.type = type;
    }

    public MxServiceQuery cloneInstance() {
        MxObjectNameQuery query =
                    (MxObjectNameQuery) super.cloneInstance();

        query.type = this.type;
        query.props = this.props;
        query.names = this.names;

        return query;
    }

    protected String getProperty(String name) {
        return getServerDetector().getTypeProperty(this.type, name);
    }

    private StringBuffer appendComma(StringBuffer buf) {
        char c = buf.charAt(buf.length() - 1);
        if ((c != ',') && (c != ':')) {
            buf.append(',');
        }
        return buf;
    }

    private boolean isAutoValue(String val) {
        return val.startsWith("%") && val.endsWith("%");
    }

    private ObjectName getObjectNameProperty() {
        String name = getProperty(PROP_OBJECT_NAME);
        if (name == null) {
            String msg =
                        this.type +
                                    " service did not define property " +
                                    PROP_OBJECT_NAME;
            throw new IllegalArgumentException(msg);
        }

        name = MxUtil.expandObjectName(name,
                    getServerDetector().getConfig());

        this.names = new ArrayList();
        ObjectName oName;

        try {
            oName = new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            String msg =
                        this.type +
                                    " service defined malformed " +
                                    PROP_OBJECT_NAME + "=" + name;
            throw new IllegalArgumentException(msg);
        }

        Map props = oName.getKeyPropertyList();
        for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();

            if (isAutoValue(val)) {
                this.names.add(key);
            }
        }

        return oName;
    }

    public String getMBeanClass() {
        return getProperty(PROP_MBEAN_CLASS);
    }

    public String getObjectNameFilter() {
        return getProperty(PROP_OBJECT_NAME_FILTER);
    }

    public String getQueryName() {
        ObjectName name = getObjectNameProperty();

        StringBuffer buf = new StringBuffer();

        buf.append(name.getDomain()).append(":");

        boolean isPattern = false;
        this.props = name.getKeyPropertyList();
        for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();

            if (isAutoValue(val)) {
                this.props.put(key, "*");
                isPattern = true;
                continue;
            }
            buf.append(key).append('=').append(val);
            if (it.hasNext()) {
                appendComma(buf);
            }
        }
        if (isPattern) {
            appendComma(buf).append('*');
        }

        return buf.toString();
    }

    // can't use queryNames w/ wildcard for property values like so:
    // jboss.ejb:service=EJB,jndiName=*
    // can't use QueryExp either because that is applied to attributes
    // of the MBean, not the attributes of the ObjectName itself
    public boolean apply(ObjectName name) {
        Map props = name.getKeyPropertyList();
        if (this.props.size() != props.size()) {
            return false;
        }
        for (Iterator it = this.props.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            String compare = (String) props.get(key);
            if (compare == null) {
                return false;
            }
            if (val.equals("*")) {
                continue;
            }
            if (!val.equals(compare)) {
                return false;
            }
        }
        return true;
    }

    public String getServiceResourceType() {
        // the name="..." attribute value from <service> tag in the plugin .xml
        return getServerDetector().getTypeNameProperty(this.type);
    }

    public Properties getResourceConfig() {
        ObjectName name = getObjectName();
        Properties config = new Properties();

        for (Iterator it = this.props.keySet().iterator(); it.hasNext();)
        {
            String key = (String) it.next();
            if (!this.props.get(key).equals("*")) {
                continue;
            }
            String val = name.getKeyProperty(key);
            if (val != null) {
                config.setProperty(key, val);
            }
        }

        return config;
    }

    public String getName() {
        return getName(true);
    }

    public String getName(boolean includeNoneKeyProperties) {
        ObjectName oName = getObjectName();
        if (oName == null) {
            return null; // XXX happens during cloneInstance()
        }

        StringBuffer name = new StringBuffer();

        for (Iterator it = this.names.iterator(); it.hasNext();) {
            String keyProperty = oName.getKeyProperty((String) it.next());
            if (!includeNoneKeyProperties && "none".equals(keyProperty)) {
                continue;
            } else {
                name.append(keyProperty);
                if (it.hasNext()) {
                    name.append(' ');
                }
            }
        }

        return name.toString();
    }

    public boolean hasControl() {
        // XXX this functionality should be elsewhere
        ProductPluginManager ppm =
                    (ProductPluginManager) getServerDetector().getManager().getParent();
        GenericPlugin plugin = ppm.getControlPlugin(this.type);
        return plugin != null;
    }
}
