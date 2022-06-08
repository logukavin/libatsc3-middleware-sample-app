package com.github.nmuzhichin.jsonrpc.internal.bijections;

import java.util.HashMap;
import java.util.Map;

class Bijection2<K, V> implements Bijections<K, V> {
    private final K key1;
    private final V value1;

    private final K key2;
    private final V value2;

    Bijection2(final K key1, final V value, final K key2, final V value2) {
        this.key1 = key1;
        this.value1 = value;

        this.key2 = key2;
        this.value2 = value2;
    }

    @Override
    public Map<K, V> toMap() {
        final HashMap<K, V> m0 = new HashMap<>();
        m0.put(key1, value1);
        m0.put(key2, value2);
        return m0;
    }
}
