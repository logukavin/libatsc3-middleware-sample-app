package com.github.nmuzhichin.jsonrpc.internal.bijections;

import java.util.HashMap;
import java.util.Map;

class Bijection4<K, V> implements Bijections<K, V> {
    private final K key1;
    private final V value1;

    private final K key2;
    private final V value2;

    private final K key3;
    private final V value3;

    private final K key4;
    private final V value4;

    Bijection4(final K key1, final V value,
               final K key2, final V value2,
               final K key3, final V value3,
               final K key4, final V value4) {

        this.key1 = key1;
        this.value1 = value;

        this.key2 = key2;
        this.value2 = value2;

        this.key3 = key3;
        this.value3 = value3;

        this.key4 = key4;
        this.value4 = value4;
    }

    @Override
    public Map<K, V> toMap() {
        final HashMap<K, V> m0 = new HashMap<>();
        m0.put(key1, value1);
        m0.put(key2, value2);
        m0.put(key3, value3);
        m0.put(key4, value4);
        return m0;
    }
}
