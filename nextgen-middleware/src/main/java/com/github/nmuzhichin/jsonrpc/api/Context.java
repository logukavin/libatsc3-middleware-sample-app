package com.github.nmuzhichin.jsonrpc.api;

import java.util.Map;

/**
 * Invocation context abstraction
 */
public interface Context {
    /**
     * Invoke method by method name and arguments.
     */
    Object invoke(String methodName, Map<String, Object> params);
}
