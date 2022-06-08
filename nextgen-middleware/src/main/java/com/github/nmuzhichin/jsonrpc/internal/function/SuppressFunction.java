package com.github.nmuzhichin.jsonrpc.internal.function;

@FunctionalInterface
public interface SuppressFunction<T, R> {
    R apply(T t) throws Throwable;
}
