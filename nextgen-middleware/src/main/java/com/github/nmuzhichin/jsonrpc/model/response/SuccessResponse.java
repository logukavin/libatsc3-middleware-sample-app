package com.github.nmuzhichin.jsonrpc.model.response;

import java.util.Objects;
import java.util.Optional;

final class SuccessResponse extends AbstractResponse {
    private final Object result;

    SuccessResponse(final Long requestId, final Object result) {
        super(requestId);
        this.result = result;
    }

    @Override
    public Object getBody() {
        return result;
    }

    @Override
    public <T> Optional<T> getSuccess(Class<T> casted) {
        try {
            return Optional.of(casted.cast(result));
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final SuccessResponse that = (SuccessResponse) o;

        return Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        return result1;
    }
}
