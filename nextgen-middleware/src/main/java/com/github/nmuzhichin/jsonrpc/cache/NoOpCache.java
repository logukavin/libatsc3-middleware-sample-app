package com.github.nmuzhichin.jsonrpc.cache;

public final class NoOpCache implements CacheProvider {
    public static final CacheProvider NO_OP_CACHE = new NoOpCache();

    private NoOpCache() {
        // Get NO_OP_CACHE
    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public void put(String key, Object value) {
        // Nothing doing
    }
}
