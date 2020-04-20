package utils.cache;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface DataCache<K, T> {
    void add(K key, @NotNull T value);
    @Nullable
    T get(K key);
    void delete(K key);
}
