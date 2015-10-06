/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.mssql;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.hyperic.hq.plugin.mssql.MsSQLMeasurementPlugin.DEFAULT_SQLSERVER_METRIC_PREFIX;
import org.hyperic.hq.pdh.MultipleCollectQueryPdh;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.sigar.Sigar;

/**
 *
 * @author laullon
 */
public class MsSQLCollector extends Collector {

    private static final Log log = LogFactory.getLog(MsSQLCollector.class);
    private final List<String> counters = new ArrayList<String>();

    @Override
    public void collect() {
        MsSQLDetector.debug(log, "[collect] (" + this + ") [" + getProperties() + "] counters.size() = " + counters.size());
        if (counters.size() > 0) {
            try {
                Map<String, Double> res = MultipleCollectQueryPdh.getFormattedValues(counters);
                for (Map.Entry<String, Double> entry : res.entrySet()) {
                    String obj = entry.getKey();
                    Double val = entry.getValue();
                    MsSQLDetector.debug(log, "(" + this + ") [collect] " + obj + " = " + val);
                    setValue(obj, val);
                }
            } catch (Exception ex) {
                MsSQLDetector.debug(log, "(" + this + ") [collect] " + ex, ex);
            }
        }
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        MsSQLDetector.debug(log, "[getValue] (" + this + ") [" + getProperties() + "] metirc = " + metric);
        MsSQLDetector.debug(log, "[getValue] (" + this + ") [" + getProperties() + "] counters.size() = " + counters.size());
        String format = metric.getProperties().getProperty("format", "1");

        String prefix = metric.getProperties().getProperty("pref_prefix");
        if (prefix == null) {
            prefix = metric.getObjectProperty("sn");
        }
        String g = metric.getProperties().getProperty("g");

        String obj;
        if (g.equals("process")) {
            obj = prepareProcessMetric(metric);
        } else if (format.equals("2")) {
            obj = "\\" + g + "\\" + metric.getAttributeName();
        } else {
            if (MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME.equalsIgnoreCase(prefix)) {
                prefix = DEFAULT_SQLSERVER_METRIC_PREFIX;
            }
            obj = "\\" + prefix + ":" + g + "\\" + metric.getAttributeName();
        }

        MetricValue res = MetricValue.NONE;
        if (obj != null) {
            if (counters.contains(obj)) {
                res = result.getMetricValue(obj);
            } else {
                counters.add(obj);
                MsSQLDetector.debug(log, "[getValue] (" + this + ") [" + getProperties() + "] new counter added counters.size() = " + counters.size());
            }
        }

        MsSQLDetector.debug(log, "[getValue] (" + this + ") [" + getProperties() + "] obj:'" + obj + "' res:'" + res.getValue() + "'");
        return res;
    }

    private String prepareProcessMetric(Metric metric) {
        String obj = null;
        try {
            MsSQLDetector.debug(log, "[ppm] metric='" + metric + "'");
            String serviceName = metric.getProperties().getProperty("service_name");
            if (serviceName == null) {
                throw new IllegalArgumentException("'service_name' is null. Metric:" + metric);
            }

            Sigar sigar = new Sigar();
            long servicePID = sigar.getServicePid(serviceName);
            MsSQLDetector.debug(log, "[ppm] serviceName='" + serviceName + "' servicePID='" + servicePID + "'");

            List<String> instances = Arrays.asList(MultipleCollectQueryPdh.getInstances("Process"));
            String serviceInstance = null;
            for (int i = 0; (i < instances.size()) && (serviceInstance == null); i++) {
                String instance = instances.get(i);
                if (instance.startsWith("sqlservr")) {
                    String idp_obj = "\\Process(" + instance + ")\\ID Process";
                    MsSQLDetector.debug(log, "[ppm] idp_obj='" + idp_obj + "'");
                    double idp = MultipleCollectQueryPdh.getValue(idp_obj);
                    if (idp == servicePID) {
                        serviceInstance = instance;
                        MsSQLDetector.debug(log, "[ppm] serviceName='" + serviceName + "' serviceInstance='" + serviceInstance + "'");
                    }
                }
            }

            if (serviceInstance != null) {
                obj = "\\Process(" + serviceInstance + ")\\" + metric.getAttributeName();
                MsSQLDetector.debug(log, "[ppm] obj = '" + obj + "'");
            } else {
                MsSQLDetector.debug(log, "[ppm] Process for serviceName='" + serviceName + "' not found, returning " + null);
            }
        } catch (Exception ex) {
            MsSQLDetector.debug(log, "[ppm] " + ex, ex);
        }
        return obj;
    }
}
