package com.github.nmuzhichin.jsonrpc.model.response;

import com.github.nmuzhichin.jsonrpc.model.Versionable;
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;

import java.util.Objects;
import java.util.Optional;

abstract class AbstractResponse implements Response {
    private final Long requestId;

    AbstractResponse(final Long requestId) {
        this.requestId = requestId;
    }

    @Override
    public String getJsonrpc() {
        return Versionable.JSON_RPC;
    }

    @Override
    public Long getId() {
        return requestId;
    }

    @Override
    public <T> Optional<T> getSuccess(Class<T> casted) {
        return Optional.empty();
    }

    @Override
    public Optional<Error> getError() {
        return Optional.empty();
    }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AbstractResponse that = (AbstractResponse) o;

        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return requestId != null ? requestId.hashCode() : 0;
    }
}
