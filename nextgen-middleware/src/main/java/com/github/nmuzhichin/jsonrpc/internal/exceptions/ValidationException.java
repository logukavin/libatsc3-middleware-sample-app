package com.github.nmuzhichin.jsonrpc.internal.exceptions;

public class ValidationException extends RuntimeException {
    public ValidationException(final String message) {
        super(message);
    }

    public ValidationException(final Throwable cause) {
        super(cause);
    }
}
