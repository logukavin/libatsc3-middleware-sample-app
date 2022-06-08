package com.github.nmuzhichin.jsonrpc.internal.asserts;

import com.github.nmuzhichin.jsonrpc.internal.exceptions.InternalProcessException;

public abstract class Assert {

    private Assert() {
        // Uses static methods
    }

    public static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new InternalProcessException(message);
        }
    }

    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new InternalProcessException(message);
        }
    }
}
