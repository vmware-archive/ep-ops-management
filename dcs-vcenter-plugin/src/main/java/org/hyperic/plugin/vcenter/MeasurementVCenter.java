package org.hyperic.plugin.vcenter;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

/**
 * Created by bharel on 4/19/2015.
 */
public class MeasurementVCenter extends SigarMeasurementPlugin {

    private static final Log log = LogFactory.getLog(MeasurementVCenter.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = MetricValue.NONE;

        Properties props = metric.getObjectProperties();
        String type = props.getProperty("Type");
        String domain = metric.getDomainName();

        if (domain.equals("sigar.ext")) {
            if (type.equals("ChildProcesses")) {
                String arg = props.getProperty("Arg");
                res = new MetricValue(getChildProcessCount(arg));
            }
        } else if (domain.equals("sigar")) {
            res = super.getValue(metric);
        } else {
            res = Collector.getValue(this, metric);
        }
        return res;
    }

    private double getChildProcessCount(String arg) throws PluginException,MetricNotFoundException {
        try {
            Sigar s = getSigar();
            long processIds[] = s.getProcList();
            ProcessQuery query = ProcessQueryFactory.getInstance().getQuery(arg);
            long parentPid = query.findProcess(s);

            double count = 0;
            for (long pid : processIds) {
                ProcState state;
                try {
                    state = s.getProcState(pid);
                    if (parentPid == state.getPpid()) {
                        count++;
                    }
                } catch (SigarException e) {
                    log.debug("[getChildProcessCount] error:" + e, e);
                }
            }
            return count;
        } catch (SigarException e) {
            log.debug("[getChildProcessCount] error:" + e, e);
            throw new MetricNotFoundException(e.getMessage(), e);
        }
    }
}
