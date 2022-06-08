package com.github.nmuzhichin.jsonrpc.context;

public interface Metadata {
    /**
     * Create the string with name for using in a cache.
     */
    String cacheName(String postfix);
}
