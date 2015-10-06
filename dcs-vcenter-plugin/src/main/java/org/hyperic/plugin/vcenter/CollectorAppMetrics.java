package org.hyperic.plugin.vcenter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import static org.hyperic.plugin.vcenter.DiscoveryVCenter.readInputString;

public class CollectorAppMetrics extends HttpColector {

    private static final int TOTAL_IDX = 3;
    private static final int NSAMPLES_IDX = 2;
    private static final int METRIC_INDEX = 1;
    private static final int VALUE_IDX = 2;

    private static final Log log = LogFactory.getLog(CollectorAppMetrics.class);
    private final Map<String, Double> previousValues = new HashMap<String, Double>();
    private String type;
    private static final Set<String> activeMetrics = new HashSet<String>();

    static {
        String[] metrics = {"operations./ActivationStats/Actv='vim.VirtualMachine.powerOn'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.powerOff'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.ManagedEntity.destroy'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.ClusterComputeResource.addHost'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.migrate'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.Folder.createVm'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.clone'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.relocate'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.unregister'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.Folder.registerVm'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.createSnapshot'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.vm.Snapshot.remove'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.host.NetworkSystem.addPortGroup'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.host.NetworkSystem.removePortGroup'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.reset'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.suspend'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.ResourcePool.createResourcePool'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.ResourcePool.moveInto'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.VirtualMachine.reconfigure'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.SessionManager.login'/Errors/total/",
            "operations./ActivationStats/Task/Actv='vim.SessionManager.logout'/Errors/total/",
            "operations./ActivationStats/Task/Actv='vim.SessionManager.login'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.SessionManager.logout'/TotalTime/",
            "operations./ActivationStats/Task/Actv='vim.SessionManager.terminate'/Errors/",
            "latency./ActivationStats/Actv='vim.VirtualMachine.powerOn'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.powerOff'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.ManagedEntity.destroy'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.ClusterComputeResource.addHost'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.migrate'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.Folder.createVm'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.clone'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.relocate'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.unregister'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.Folder.registerVm'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.createSnapshot'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.vm.Snapshot.remove'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.host.NetworkSystem.addPortGroup'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.host.NetworkSystem.removePortGroup'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.reset'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.suspend'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.ResourcePool.createResourcePool'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.ResourcePool.moveInto'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.VirtualMachine.reconfigure'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.SessionManager.logout'/TotalTime/",
            "latency./ActivationStats/Task/Actv='vim.SessionManager.login'/TotalTime/",
            "/InventoryStats/ManagedEntityStats/Clusters/total",
            "/InventoryStats/ManagedEntityStats/Datacenters/total",
            "/InventoryStats/ManagedEntityStats/Datastores/total",
            "/InventoryStats/ManagedEntityStats/NumHosts/total",
            "/InventoryStats/ManagedEntityStats/NumPoweredOnVms/total",
            "/InventoryStats/ManagedEntityStats/NumVirtualMachines/total",
            "/InventoryStats/ManagedEntityStats/ResourcePools/total",
            "/AlarmStats/NotificationsPending/Count/total"};
        activeMetrics.addAll(Arrays.asList(metrics));
    }

    @Override
    protected void init() throws PluginException {
        super.init();
        this.host = "localhost";
        this.ssl = true;
        this.port = 443;

        type = getProperties().getProperty("type", "avail");
        if (type.equals("push")) {
            this.path = "/vod/index.html?page=pushCounters";
        } else if (type.equals("pull")) {
            this.path = "/vod/index.html?page=pullCounters";
        } else {
            this.path = "/vod/index.html";
        }
        log.debug("[init] type='" + type + "' path='" + path + "'");
    }

    @Override
    public void collect() {
        log.debug("[collect] start");

        if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
            log.error("[collect] no VCenter credentials.");
            setAvailability(Metric.AVAIL_UNKNOWN);
        } else {
            super.collect();
        }
    }

    @Override
    protected void analyzeResponse(HttpResponse response) throws IOException {
        String html = readInputString(response.getEntity().getContent());
        log.debug("[analyzeResponse] type='" + type + "'");

        if (html != null) {
            if (type.equals("push")) {
                Matcher matcher = Pattern.compile("<tr>[^<]*<td>([^<]*)</td>[^<]*<td>([^<]*)</td>[^<]*<td>([^<]*)</td>[^<]*<td>([^<]*)</td>[^<]*<td>([^<]*)</td>[^<]*<td>([^<]*)</td>[^<]*</tr>").matcher(html);
                while (matcher.find()) {
                    String numSamplesKey = "numSamples." + matcher.group(METRIC_INDEX);
                    String operationsKey = "operations." + matcher.group(METRIC_INDEX);
                    String latencyKey = "latency." + matcher.group(METRIC_INDEX);
                    String totalKey = "total." + matcher.group(METRIC_INDEX);

                    double n2 = Double.valueOf(matcher.group(NSAMPLES_IDX));
                    if (activeMetrics.contains(operationsKey)) {
                        setValue(operationsKey, n2);
                    }

                    if (activeMetrics.contains(latencyKey)) {
                        double t2 = Double.valueOf(matcher.group(TOTAL_IDX));
                        Double prevNumSamples = previousValues.get(numSamplesKey);
                        if (prevNumSamples != null) {
                            double n1 = prevNumSamples;
                            double t1 = previousValues.get(totalKey);
                            if ((n2 - n1) != 0) {
                                double t = (t2 - t1) / (n2 - n1);
                                setValue(latencyKey, t);
                                log.debug("[analyzeResponse] '" + latencyKey + "' n1:'" + n1 + "' n2:'" + n2 + "' t1:'" + t1 + "' t2:'" + t2 + "'' t:'" + t + "'");
                            } else {
                                setValue(latencyKey, 0);
                            }
                        } else {
                            log.debug("[analyzeResponse] '" + latencyKey + "' n2:'" + n2 + "' t2:'" + t2 + "'");
                            setValue(latencyKey, 0);
                        }
                        previousValues.put(numSamplesKey, n2);
                        previousValues.put(totalKey, t2);
                    }
                }
            } else if (type.equals("pull")) {
                Matcher matcher = Pattern.compile("<tr>[^<]*<td>([^<]*)</td>[^<]*<td>([^<]*)</td>[^<]*</tr>").matcher(html);
                while (matcher.find()) {
                    String metric = matcher.group(METRIC_INDEX);
                    if (activeMetrics.contains(metric)) {
                        setValue(metric, Double.parseDouble(matcher.group(VALUE_IDX)));
                    }
                }
            }
        }

        EntityUtils.consume(response.getEntity());
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        MetricValue res = super.getValue(metric, result);
        if ((res != null) && Double.isNaN(res.getValue())) {
            res = new MetricValue(0);
        }
        return res;
    }
}
