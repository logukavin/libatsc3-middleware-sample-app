package com.github.nmuzhichin.jsonrpc.normalizer;

import com.github.nmuzhichin.jsonrpc.internal.logger.Logger;

public final class WithoutNormalization implements ValueNormalization {
    public static final WithoutNormalization WITHOUT_NORMALIZATION = new WithoutNormalization();
    private static final Logger log = Logger.of(WithoutNormalization.class);

    private WithoutNormalization() {
        // Singleton
    }

    @Override
    public Object normalize(final Object nonNormalValue, final Class<?> objectType) {
        log.warn("Argument {} without normalization", objectType);
        return nonNormalValue;
    }
}
