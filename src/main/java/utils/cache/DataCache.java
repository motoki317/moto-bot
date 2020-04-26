package utils.cache;

import javax.annotation.Nullable;

public interface DataCache<K, T> {
    void add(K key, @Nullable T value);
    boolean exists(K key);
    @Nullable
    T get(K key);
    void delete(K key);
}
