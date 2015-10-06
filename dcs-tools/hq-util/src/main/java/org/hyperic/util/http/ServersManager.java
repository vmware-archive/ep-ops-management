package org.hyperic.util.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.hyperic.util.timer.StopWatch;

public class ServersManager {

    private static final String SERVICE_UNAVAILABLE = "Service Unavailable";
    private static final Log logger = LogFactory.getLog(ServersManager.class);

    private List<Server> servers = new ArrayList<Server>();
    private Server alternateServer = null;
    private Server preferredServer = null;
    private boolean isGlobalDown = false;
    private long globalDownPeriod;
    private StopWatch globalDownPeriodWatch;
    private CommunicationConfiguration config = null;
    private HQHttpClient client = null;

    public ServersManager(HQHttpClient client,
                          CommunicationConfiguration communicationConfiguration) {
        this.config = communicationConfiguration;
        logger.info(communicationConfiguration);
        this.client = client;
        this.globalDownPeriodWatch = new StopWatch();
    }

    private List<Server> buildServers(String url)
        throws UnknownHostException,
        MalformedURLException {
        URL notResolvedUrl = new URL(url);
        // With DNS cache TTL set to -1, the IP list is cached at the JVM,
        // making this call highly cheap.
        String hostname = notResolvedUrl.getHost();
        InetAddress[] addresses = InetAddress.getAllByName(hostname);
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException(String.format(
                        "The host %d is not reachable", hostname));
        }
        List<Server> resolvedServers = new LinkedList<Server>();
        preferredServer = new Server(addresses[0], client, config);
        if (!config.isSupportRRDNS()) {
            logger.info("Agent does not support RRDNS");
            resolvedServers = Collections.emptyList();
        } else {
            logger.info("Agent support RRDNS");
            for (int i = 1; i < addresses.length; i++) {
                resolvedServers.add(new Server(addresses[i], client, config));
            }
            // Create a random selection of the alternates servers at a fail over.
            // DNS server with cyclic order will always return the same alternate ips list
            // to the same preferred ip
            Collections.shuffle(resolvedServers);

        }
        logger.info("Servers are : Preferred(" + preferredServer + ") - "
                    + resolvedServers);
        return resolvedServers;
    }

    public synchronized HttpResponse send(AgentRequest request)
        throws IOException {
        // Lazy evaluation of the servers
        if (preferredServer == null) {
            this.servers = buildServers(request.getUrl());
        }
        if (isGlobalDownOn()) {
            // Agent on global down time.
            logger.info("Servers are in global down  - time elapsed ("
                        + TimeUnit.MILLISECONDS.toMinutes(globalDownPeriodWatch
                                    .getElapsed()) + ")");
            throw new IOException(SERVICE_UNAVAILABLE);
        }
        // global down is off
        return sendToServer(request);
    }

    private boolean isGlobalDownOn() {
        if (!isGlobalDown) {
            return false;
        }
        // Is down time elapsed
        if (globalDownPeriodWatch.getElapsed() < globalDownPeriod) {
            return true;
        }

        // Global down time is over
        isGlobalDown = false;
        return false;
    }

    private HttpResponse sendToServer(AgentRequest request)
        throws IOException {
        HttpResponse response = null;
        // First try the preferred server - for affinity
        if (preferredServer.isAvailable()) {
            response = preferredServer.send(request);
            alternateServer = null;
            // Try the alternate server if exists
        } else if (alternateServer != null && alternateServer.isAvailable()) {
            response = alternateServer.send(request);
            // Find an alternate only if there are servers in the servers list
        } else if (!servers.isEmpty()) {
            response = findAlternateServerAndSend(request);
        } else {
            throw new IOException(SERVICE_UNAVAILABLE);
        }
        return response;

    }

    private HttpResponse findAlternateServerAndSend(AgentRequest request)
        throws IOException {
        alternateServer = null;
        IOException lastException = null;
        for (Server server : servers) {
            try {
                HttpResponse response = server.send(request);
                alternateServer = server;
                logger.info("Choose new alternate server " + alternateServer);
                return response;
            } catch (IOException e) {
                logger.info("Failed trying server " + server);
                lastException = e;
            }
        }
        // Turn global down on when there are no servers available
        logger.error("All Servers are not available");
        setGlobalDownOn();
        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException(SERVICE_UNAVAILABLE);
        }

    }

    private void setGlobalDownOn() {
        globalDownPeriod = preferredServer.getDownPeriod();
        logger.error("Insert to Global down for a period of " + TimeUnit.MILLISECONDS.toMinutes(globalDownPeriod)
                    + " minutes");
        isGlobalDown = true;
        globalDownPeriodWatch.reset();
    }

    public synchronized HttpResponse sendTryAll(AgentRequest request)
        throws IOException {

        if (preferredServer == null) {
            servers = buildServers(request.getUrl());
            servers.add(0, preferredServer);
        }
        Server lastSuccessServer = null;
        int counter = 0;
        String lastError = request.getUrl() + " is not reachable";
        for (Server server : servers) {
            try {
                counter++;
                HttpResponse response = server.send(request);
                lastSuccessServer = server;
                logger.info("Successfully sent to server " + lastSuccessServer);
                return response;
            } catch (IOException e) {
                // If we got IO exception and this is the last ip in the list,
                // thow the exception
                if (counter == servers.size()) {
                    throw e;
                }
            } finally {
                // Move the last successful server to the head of the servers list
                if (lastSuccessServer != null) {
                    servers.remove(lastSuccessServer);
                    servers.add(0, lastSuccessServer);
                    lastSuccessServer = null;
                }
            }
        }
        // We get here, after we tried all the nodes ip addresses, we must throw
        // an exception
        throw new IOException(lastError);
    }

}
