package com.github.nmuzhichin.jsonrpc.normalizer;

/**
 * Normalizes Map to the appropriate object with the specified class.
 */
@FunctionalInterface
public interface ValueNormalization {
    Object normalize(final Object nonNormalValue, final Class<?> objectType);
}
