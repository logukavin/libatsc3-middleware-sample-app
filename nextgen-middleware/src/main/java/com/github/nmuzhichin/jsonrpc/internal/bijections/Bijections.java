package com.github.nmuzhichin.jsonrpc.internal.bijections;

import com.github.nmuzhichin.jsonrpc.internal.asserts.Assert;

import java.util.HashMap;
import java.util.Map;

public interface Bijections<K, V> {

    static <K, V> Bijections<K, V> of(K key, V value) {
        Assert.requireNotNull(key, "Key must be present.");
        Assert.requireNotNull(value, "Value must be present.");

        return new Bijection<>(key, value);
    }

    static <K, V> Bijections<K, V> of(K key1, V value1,
                                      K key2, V value2) {
        Assert.requireNotNull(key1, "Key must be present.");
        Assert.requireNotNull(value1, "Value must be present.");

        Assert.requireNotNull(key2, "Key must be present.");
        Assert.requireNotNull(value2, "Value must be present.");

        return new Bijection2<>(key1, value1,
                                key2, value2);
    }

    static <K, V> Bijections<K, V> of(K key1, V value1,
                                      K key2, V value2,
                                      K key3, V value3) {

        Assert.requireNotNull(key1, "Key must be present.");
        Assert.requireNotNull(value1, "Value must be present.");

        Assert.requireNotNull(key2, "Key must be present.");
        Assert.requireNotNull(value2, "Value must be present.");

        Assert.requireNotNull(key3, "Key must be present.");
        Assert.requireNotNull(value3, "Value must be present.");

        return new Bijection3<>(key1, value1,
                                key2, value2,
                                key3, value3);
    }

    static <K, V> Bijections<K, V> of(K key1, V value1,
                                      K key2, V value2,
                                      K key3, V value3,
                                      K key4, V value4) {

        Assert.requireNotNull(key1, "Key must be present.");
        Assert.requireNotNull(value1, "Value must be present.");

        Assert.requireNotNull(key2, "Key must be present.");
        Assert.requireNotNull(value2, "Value must be present.");

        Assert.requireNotNull(key3, "Key must be present.");
        Assert.requireNotNull(value3, "Value must be present.");

        Assert.requireNotNull(key4, "Key must be present.");
        Assert.requireNotNull(value4, "Value must be present.");

        return new Bijection4<>(key1, value1,
                                key2, value2,
                                key3, value3,
                                key4, value4);
    }

    static <K, V> Bijections<K, V> of(K key1, V value1,
                                      K key2, V value2,
                                      K key3, V value3,
                                      K key4, V value4,
                                      K key5, V value5) {
        Assert.requireNotNull(key1, "Key must be present.");
        Assert.requireNotNull(value1, "Value must be present.");

        Assert.requireNotNull(key2, "Key must be present.");
        Assert.requireNotNull(value2, "Value must be present.");

        Assert.requireNotNull(key3, "Key must be present.");
        Assert.requireNotNull(value3, "Value must be present.");

        Assert.requireNotNull(key4, "Key must be present.");
        Assert.requireNotNull(value4, "Value must be present.");

        Assert.requireNotNull(key5, "Key must be present.");
        Assert.requireNotNull(value5, "Value must be present.");

        return new Bijection5<>(key1, value1,
                                key2, value2,
                                key3, value3,
                                key4, value4,
                                key5, value5);
    }

    static <K, V> Bijections<K, V> of(Map<K, V> keyValueN) {
        Assert.requireNotNull(keyValueN, "keyValueN must be present.");

        return new BijectionN<>(keyValueN);
    }

    static <V> Bijections<String, V> of(V[] objects) {
        final Map<String, V> n = new HashMap<>(objects.length);

        for (final V object : objects) {
            n.put(object.getClass().getName(), object);
        }

        return new BijectionN<>(n);
    }

    Map<K, V> toMap();
}
