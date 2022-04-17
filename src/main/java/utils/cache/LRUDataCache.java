package utils.cache;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUDataCache<K, T> implements DataCache<K, T> {
    private static class LRU<K, T> extends LinkedHashMap<K, T> {
        private final int maxRecords;

        private LRU(int maxRecords) {
            super(maxRecords, 0.75f, true);
            this.maxRecords = maxRecords;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, T> eldest) {
            return size() > maxRecords;
        }
    }

    private final LRU<K, T> lru;

    /**
     * Creates a new LRU cache.
     *
     * @param maxRecords Number of records to hold.
     */
    public LRUDataCache(int maxRecords) {
        this.lru = new LRU<>(maxRecords);
    }

    @Override
    public synchronized void add(K key, @Nullable T value) {
        this.lru.put(key, value);
    }

    @Override
    public boolean exists(K key) {
        return this.lru.containsKey(key);
    }

    @Nullable
    @Override
    public T get(K key) {
        return this.lru.get(key);
    }

    @Override
    public void delete(K key) {
        this.lru.remove(key);
    }
}
