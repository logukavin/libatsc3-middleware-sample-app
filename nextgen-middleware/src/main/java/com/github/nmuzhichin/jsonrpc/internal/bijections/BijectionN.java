package com.github.nmuzhichin.jsonrpc.internal.bijections;

import java.util.Map;

class BijectionN<K, V> implements Bijections<K, V> {
    private final Map<K, V> n;

    BijectionN(final Map<K, V> n) {
        this.n = n;
    }

    @Override
    public Map<K, V> toMap() {
        return n;
    }
}
