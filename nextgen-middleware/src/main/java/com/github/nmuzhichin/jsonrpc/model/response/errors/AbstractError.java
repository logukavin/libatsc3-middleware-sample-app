package com.github.nmuzhichin.jsonrpc.model.response.errors;

public abstract class AbstractError<T> implements Error {
    private final int code;
    private final String message;

    AbstractError(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public abstract T getData();

    public abstract void dropData();
}
