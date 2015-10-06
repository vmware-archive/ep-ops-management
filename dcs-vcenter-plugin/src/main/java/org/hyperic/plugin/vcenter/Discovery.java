/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vcenter;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author glaullon
 */
public class Discovery extends DaemonDetector {

    private static final Log log = LogFactory.getLog(Discovery.class);

    @Override
    protected List<ServiceResource> discoverServices(ConfigResponse config)
            throws PluginException {

        List<ServiceResource> res = new ArrayList();
        TypeInfo[] types = this.getPluginData().getTypes();

        for (TypeInfo type : types) {
            if (type.getType() != TypeInfo.TYPE_SERVICE) {
                continue;
            }
            if (!type.getName().startsWith(getTypeInfo().getName())) {
                continue;
            }
            if (!this.getTypeInfo().getVersion().equals(type.getVersion())) {
                continue;
            }

            log.debug("[discoverServices] '" + type.getName() + "' on '" + getTypeInfo().getName() + "'");
            boolean valid = true;

            String query = getServiceProcessQuery(type);
            if (query != null) {
                log.debug("[discoverServices] PROC_QUERY='" + query + "'");
                long[] pids = getPids(query);
                log.debug("[discoverServices] Found '" + ((pids != null) ? pids.length : 0) + "' process");
                if ((pids == null) || (pids.length == 0)) {
                    valid = false;
                }
            }

            if (valid) {
                ServiceResource service = new ServiceResource();
                service.setType(type.getName());
                String name = getTypeNameProperty(type.getName());
                service.setServiceName(name);
                setProductConfig(service, new ConfigResponse());
                setMeasurementConfig(service, new ConfigResponse());
                res.add(service);
                log.debug("[discoverServices] '" + type.getName() + "' created");
            } else {
                log.debug("[discoverServices] '" + type.getName() + "' ignored");
            }
        }

        return res;
    }

    protected String getServiceProcessQuery(TypeInfo type) {
        String res = null;
        if (isWin32()) {
            res = getTypeProperty(type.getName(), "WIN32_PROC_QUERY");
        }
        if (res == null) {
            res = getTypeProperty(type.getName(), "PROC_QUERY");
        }
        return res;
    }
}
