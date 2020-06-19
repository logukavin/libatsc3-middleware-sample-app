package org.ngbp.jsonrpc4jtestharness.models;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class JsonRpcRequest <T> {
    private Long id;
    private String method;
    private T params;
    private String jsonrpc = "2.0";

    public JsonRpcRequest() {
    }

    public JsonRpcRequest(T inputModel) {
        this.id = new Random().nextLong();


        this.params = inputModel;
    }

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

    public T getParams() {
        return params;
    }

    public void setParams(T params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "\n{" +
                "\n  id: " + id +
                "\n, method: '" + method + '\'' +
                "\n, params: " + params +
                "\n, jsonrpc: '" + jsonrpc + '\n' +
                '}';
    }
}
