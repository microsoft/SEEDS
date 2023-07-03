package com.microsoft.seeds.place.models.cache;

import java.util.List;
import java.util.Optional;

public interface Cache<K, V> {
    boolean put(K key, V value);
    Optional<V> get(K key);
    int size();
    boolean isEmpty();
    void clear();
    List<K> getAllKeys();
}
