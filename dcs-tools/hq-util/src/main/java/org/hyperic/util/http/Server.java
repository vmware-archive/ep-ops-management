package org.hyperic.util.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.util.EntityUtils;
import org.hyperic.util.timer.StopWatch;

public class Server {

    private static final String UNAUTHORIZED_AGENT_DENIED_ERROR_MSG = "Unauthorized agent denied";
    private static final String LATHER_ERROR_HEADER = "X-error-response";

    static {
        Set<Integer> serverNotReachableHttpCodes = new HashSet<Integer>();
        serverNotReachableHttpCodes.add(HttpStatus.SC_SERVICE_UNAVAILABLE);
        serverNotReachableHttpCodes.add(HttpStatus.SC_NOT_FOUND);
        SERVER_NOT_REACHABLE_HTTP_CODES = Collections
                    .unmodifiableSet(serverNotReachableHttpCodes);
    }

    private final static Set<Integer> SERVER_NOT_REACHABLE_HTTP_CODES;

    private long lastSuccessTimestamp;
    private final InetAddress ipAddress;
    private boolean isDown = false;
    private StopWatch downPeriodWatch;
    private long downPeriod;
    private long failPeriod;
    private Random random = null;
    private int downPeriodInterval;
    private int minDownPeriodIntervalInMin = 5;
    private HQHttpClient client = null;

    private static final Log logger = LogFactory.getLog(Server.class);

    public Server(InetAddress ipAddress,
                  HQHttpClient client,
                  CommunicationConfiguration config) {
        this.ipAddress = ipAddress;
        this.client = client;
        this.lastSuccessTimestamp = System.currentTimeMillis();
        this.downPeriodWatch = new StopWatch();
        this.failPeriod = TimeUnit.MINUTES
                    .toMillis(config.getFailPeriodInMin());
        this.downPeriodInterval = config.getDownPeriodIntervalInMin();
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {// if no provider supports a SecureRandomSpi implementation for the
                                              // specified algorithm
            random = new SecureRandom(); // then use the default random number algorithm.
        }

    }

    public boolean isAvailable() {
        if (isDown) {
            return isDownTimeElapsed();
        }
        return isServerAccessible();
    }

    private boolean isServerAccessible() {
        if (System.currentTimeMillis() >= lastSuccessTimestamp + failPeriod) {
            isDown = true;
            downPeriod = getDownPeriod();
            downPeriodWatch.reset();
            logger.error("Server is not available for a period of "
                        + TimeUnit.MILLISECONDS.toMinutes(failPeriod)
                        + " minutes -  Mark " + this + " as down for a period of "
                        + TimeUnit.MILLISECONDS.toMinutes(downPeriod) + " minutes");
            return false;
        }
        return true;
    }

    public long getDownPeriod() {
        int downPeriodInMin = random.nextInt(downPeriodInterval) + minDownPeriodIntervalInMin;
        return TimeUnit.MINUTES.toMillis(downPeriodInMin);
    }

    private boolean isDownTimeElapsed() {
        if (downPeriodWatch.getElapsed() < downPeriod) {
            return false;
        } else {
            isDown = false;
            logger.info("Down period end - wake up server " + this);
            return true;
        }
    }

    private String buildUrl(String originalUrl)
        throws MalformedURLException {
        URL notResolvedUrl = new URL(originalUrl);
        URL newUrl = new URL(notResolvedUrl.getProtocol(),
                    ipAddress.getHostAddress(), notResolvedUrl.getPort(),
                    notResolvedUrl.getFile());
        return newUrl.toString();

    }

    public HttpResponse send(AgentRequest request)
        throws ClientProtocolException, IOException {
        String resolvedUrl = this.buildUrl(request.getUrl());
        HttpResponse response = null;
        switch (request.getMethod()) {
            case GET:
                response = client.get(resolvedUrl, request.getHeaders());
                break;
            case POST:
                response = client
                            .post(resolvedUrl, request.getHeaders(), request.getParams());
                break;
            default:
                throw new IOException("Request Method " + request.getMethod() + " is not supported");
        }
        // If no response
        if (response == null || response.getStatusLine() == null) {
            throw new IOException(request.getUrl() + " is not reachable");
        }
        // If server is not reachable, consume the response
        if (SERVER_NOT_REACHABLE_HTTP_CODES.contains(response.getStatusLine()
                    .getStatusCode())) {
            EntityUtils.consume(response.getEntity());
            throw new IOException(response.getStatusLine().getReasonPhrase());
        }
        if (isAgentUnauthorized(response)) {
            String msg = String.format("Unable to invoke method '%s'. Unauthorized agent.", request.getMethod());
            throw new IOException(msg);
        }

        lastSuccessTimestamp = System.currentTimeMillis();
        isDown = false;
        return response;

    }

    private boolean isAgentUnauthorized(HttpResponse response)
        throws IOException {

        if (response == null || response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            return false;
        }

        Header errHeader = response.getFirstHeader(LATHER_ERROR_HEADER);

        if (errHeader == null) {
            return false;
        }

        // We need to buffer the entity, since we need to read it once to get the body.
        // If everything is good (no errors), then the rest of the code relies on the
        // fact that the content can be read from it.
        BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(response.getEntity());
        response.setEntity(bufferedEntity);

        String responseBody = EntityUtils.toString(bufferedEntity);

        if (responseBody.contains(UNAUTHORIZED_AGENT_DENIED_ERROR_MSG)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server[").append("IP:").append(ipAddress.getHostAddress())
                    .append("  ").append("isDown:").append(isDown).append("  ")
                    .append("lastSuccess:").append(new Date(lastSuccessTimestamp))
                    .append("  ").append("]");
        return sb.toString();

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Server other = (Server) obj;
        if (ipAddress == null) {
            if (other.ipAddress != null) {
                return false;
            }
        } else if (!ipAddress.equals(other.ipAddress)) {
            return false;
        }
        return true;
    }

}
