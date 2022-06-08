package com.github.nmuzhichin.jsonrpc.cache;

/**
 * Provide access to cache implementation.
 * <p>
 * The <code>jsonrpc-cache-extension</code> contains predefined
 * implementations for the most used cache providers.
 * <p>
 * By default, the {@link NoOpCache} provider without
 * implementations is used.
 *
 * @see CaffeineCache
 * @see GuavaCache
 * @see JavaxCache
 * @see NoOpCache
 */
public interface CacheProvider {
    /**
     * Returns the raw value to which the specified key is mapped,
     * or {@code null} if this cache contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped
     */
    Object get(String key);

    /**
     * Associates the {@code value} with the {@code key} in this cache. If the cache previously
     * contained a value associated with the {@code key}, the old value is replaced by the new
     * {@code value}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException if key is null or if value is null
     */
    void put(String key, Object value);
}
