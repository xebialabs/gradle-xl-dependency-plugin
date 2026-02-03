package com.xebialabs.gradle.dependency;

import java.util.HashMap;
import java.util.function.Function;

/**
 * A HashMap that computes a default value using a function when a key is not found.
 * Replacement for Groovy's Map.withDefault { } pattern.
 */
class DefaultValueMap<K, V> extends HashMap<K, V> {

    private final Function<K, V> defaultValueFunction;

    public DefaultValueMap(Function<K, V> defaultValueFunction) {
        super();
        this.defaultValueFunction = defaultValueFunction;
    }

    @Override
    public V get(Object key) {
        V value = super.get(key);
        if (value == null && !containsKey(key)) {
            @SuppressWarnings("unchecked")
            K typedKey = (K) key;
            value = defaultValueFunction.apply(typedKey);
            // Note: We don't auto-put the value like Groovy does, to match the original behavior more closely
        }
        return value;
    }
}
