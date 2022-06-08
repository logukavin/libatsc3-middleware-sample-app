package com.github.nmuzhichin.jsonrpc.model;

public interface Identifiable {

    /**
     * An identifier established by the Client that MUST contain a Number, or NULL value if included.
     * If it is not included it is assumed to be a notification.
     * The value SHOULD normally not be Null and Numbers SHOULD NOT contain fractional parts.
     */
    Long getId();
}
