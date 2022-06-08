package com.github.nmuzhichin.jsonrpc.internal.exceptions;

public class InternalProcessException extends RuntimeException {

    public InternalProcessException(String message) {
        super(message);
    }

    public InternalProcessException(Throwable cause) {
        super(cause);
    }
}
