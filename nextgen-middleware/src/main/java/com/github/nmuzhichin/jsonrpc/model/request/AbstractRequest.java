package com.github.nmuzhichin.jsonrpc.model.request;

import com.github.nmuzhichin.jsonrpc.internal.asserts.Assert;
import com.github.nmuzhichin.jsonrpc.model.Versionable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractRequest implements Request {
    private final String method;
    private final Map<String, Object> params;

    AbstractRequest(final String jsonrpc, final String method, final Map<String, Object> params) {
        Assert.requireTrue(Versionable.JSON_RPC.equals(jsonrpc), "Incorrect jsonrpc version.");
        Assert.requireNotNull(method, "Method name must be present.");

        this.method = method;
        this.params = Collections.unmodifiableMap(params == null ? new HashMap<>(1) : params);
    }

    @Override
    public String getJsonrpc() {
        return Versionable.JSON_RPC;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    abstract Request copy(Map<String, Object> params);

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AbstractRequest that = (AbstractRequest) o;

        return Objects.equals(method, that.method) && Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }
}