package com.github.nmuzhichin.jsonrpc.internal.bijections;

import java.util.Collections;
import java.util.Map;

class Bijection<K, V> implements Bijections<K, V> {
    private final K key;
    private final V value;

    Bijection(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public Map<K, V> toMap() {
        return Collections.singletonMap(key, value);
    }
}
