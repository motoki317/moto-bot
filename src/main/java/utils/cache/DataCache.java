package utils.cache;

import javax.annotation.Nullable;

public interface DataCache<K, T> {
    void add(K key, T value);
    @Nullable
    T get(K key);
}
