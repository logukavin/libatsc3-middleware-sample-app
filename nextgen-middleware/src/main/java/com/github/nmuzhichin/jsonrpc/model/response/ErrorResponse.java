package com.github.nmuzhichin.jsonrpc.model.response;

import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;

import java.util.Objects;
import java.util.Optional;

final class ErrorResponse extends AbstractResponse {
    private final Error error;

    ErrorResponse(final Long requestId, final Error error) {
        super(requestId);
        this.error = error;
    }

    @Override
    public Error getBody() {
        return error;
    }

    @Override
    public Optional<Error> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ErrorResponse that = (ErrorResponse) o;

        return Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }
}
