package com.github.nmuzhichin.jsonrpc.model.request;

import java.util.Map;
import java.util.Objects;

public final class CompleteRequest extends AbstractRequest {
    private final Long id;

    public CompleteRequest(final String jsonrpc, final Long id, final String method, final Map<String, Object> params) {
        super(jsonrpc, method, params);
        this.id = id;
    }

    public CompleteRequest(final Long id, final String method, final Map<String, Object> params) {
        this("2.0", id, method, params);
    }

    public CompleteRequest(final Long id, final String method) {
        this("2.0", id, method, null);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    CompleteRequest copy(final Map<String, Object> params) {
        return new CompleteRequest(this.getId(), this.getMethod(), params);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final CompleteRequest that = (CompleteRequest) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
