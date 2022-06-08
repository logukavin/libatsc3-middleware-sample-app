package com.github.nmuzhichin.jsonrpc.model.request;

import java.util.Map;

public final class Notification extends AbstractRequest {

    public Notification(final String jsonrpc, final String method, final Map<String, Object> params) {
        super(jsonrpc, method, params);
    }

    public Notification(final String method, final Map<String, Object> params) {
        this("2.0", method, params);
    }

    public Notification(final String method) {
        this("2.0", method, null);
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    Notification copy(final Map<String, Object> params) {
        return new Notification(this.getMethod(), params);
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
