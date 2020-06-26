package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import java.util.Map;

public class RequestParams {
    private Long id;
    private String method;
    private Map<String,Object> params;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
