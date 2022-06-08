package com.github.nmuzhichin.jsonrpc.internal.function;

import com.github.nmuzhichin.jsonrpc.internal.exceptions.InternalProcessException;

/**
 * Suppress all exceptions in lambda.
 * Try get result by {@link #suppress()} method without checked exception.
 *
 * @param <T>
 */
@FunctionalInterface
public interface ExceptionSuppressor<T> {
    T apply() throws Throwable;

    default T suppress() {
        try {
            return apply();
        } catch (Throwable throwable) {
            throw new InternalProcessException(throwable);
        }
    }

    default <E extends Throwable> void rethrow() throws E {
        try {
            apply();
        } catch (Throwable throwable) {
            throw (E) throwable;
        }
    }

    default <R> ExceptionSuppressor<R> map(SuppressFunction<T, R> function) {
        return () -> function.apply(apply());
    }
}
