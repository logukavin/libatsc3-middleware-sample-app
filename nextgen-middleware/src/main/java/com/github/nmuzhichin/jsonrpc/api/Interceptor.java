package com.github.nmuzhichin.jsonrpc.api;

public interface Interceptor<IN, ARG, OUT> {
    OUT intercept(IN in, ARG arg);
}
