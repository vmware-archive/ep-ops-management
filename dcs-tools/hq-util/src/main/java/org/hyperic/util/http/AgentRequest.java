package org.hyperic.util.http;

import java.util.HashMap;
import java.util.Map;

public class AgentRequest {

    public enum AgentHttpMethod {
        GET, POST
    }

    private final String url;
    private final AgentHttpMethod method;
    private Map<String, String> headers = new HashMap<String, String>();
    private Map<String, String> params = new HashMap<String, String>();

    public AgentRequest(String url,
                        AgentHttpMethod method) {
        this.url = url;
        this.method = method;

    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public AgentHttpMethod getMethod() {
        return method;
    }

}
