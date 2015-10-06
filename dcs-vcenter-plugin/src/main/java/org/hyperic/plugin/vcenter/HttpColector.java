package org.hyperic.plugin.vcenter;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public class HttpColector extends Collector {

    private static final Log log = LogFactory.getLog(HttpColector.class);
    private static final AgentKeystoreConfig ksCFG = new AgentKeystoreConfig();
    private HQHttpClient httpClient;
    protected String host;
    protected int port;
    protected boolean ssl;
    protected String path;
    protected String user;
    protected String pass;

    @Override
    protected void init() throws PluginException {
        super.init();
        
        log.debug("[init]");
        host = getProperties().getProperty("hostname");
        try {
            port = Integer.parseInt(getProperties().getProperty("port", "0"));
        } catch (Throwable e) {
            log.debug("[init] error parsing ''port': ", e);
        }
        ssl = "true".equalsIgnoreCase(getProperties().getProperty("ssl"));
        path = getProperties().getProperty("path");
        user = getProperties().getProperty("user");
        pass = getProperties().getProperty("pass");
    }

    @Override
    public void collect() {
        int hashCode = hashCode();
        log.debug("[collect (" + hashCode + ")] start");

        HttpHost targetHost = new HttpHost(host, port, ssl ? "https" : "http");
        if (httpClient == null) {
            httpClient = new HQHttpClient(ksCFG, new HttpConfig(10000, 10000, null, 0),
                    ksCFG.isAcceptUnverifiedCert());
            log.debug("[collect (" + hashCode + ")] httpClient created: " + httpClient);
            if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(pass)) {
                httpClient.getCredentialsProvider().setCredentials(
                        new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                        new UsernamePasswordCredentials(user, pass));
                log.debug("[collect (" + hashCode + ")] AuthScope created");
            }
        }

        HttpGet get = new HttpGet(targetHost.toURI() + path);
        log.debug("[collect (" + hashCode + ")] GET: " + get.getURI());
        try {
            long start = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(get);
            long time = System.currentTimeMillis() - start;
            int statusCode = response.getStatusLine().getStatusCode();
            setValue("ResponseTime", (double) time);
            setValue("ResponseCode", (double) statusCode);

            analyzeResponse(response);

            if (statusCode == HttpStatus.SC_OK) {
                log.debug("[collect (" + hashCode + ")] GET OK (" + statusCode + ") ResponseTime: " + time + "Ms.");
                setAvailability(true);
            } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                setAvailability(Metric.AVAIL_UNKNOWN);
            } else {
                setAvailability(false);
                log.debug("[collect (" + hashCode + ")] GET failed: (" + statusCode + ") " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException ex) {
            setAvailability(false);
            httpClient = null;
            log.debug("[collect (" + hashCode + ")] " + ex, ex);
        }

        log.debug("[collect (" + hashCode + ")] stop");
    }

    protected void analyzeResponse(HttpResponse response) throws IOException {
        EntityUtils.consume(response.getEntity());
    }
}
