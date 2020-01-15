package utils.cache;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class HashMapDataCache<K, T> implements DataCache<K, T> {
    private static class Record<T> {
        private T data;
        private long createdAt;

        Record(T data, long createdAt) {
            this.data = data;
            this.createdAt = createdAt;
        }
    }

    private final int maxRecords;
    private final long maxHoldTime;

    private final Object dataLock;
    private final Map<K, Record<T>> dataMap;

    /**
     * Creates a new map data cache system.
     * @param maxRecords Number of max records to hold. If number of records exceed this value,
     *                  records are deleted from the oldest.
     * @param maxHoldTime Maximum hold time in ms. If record was added before max hold time from now, deletes the record.
     * @param clearInterval Clear interval in ms. Clears old records with this interval.
     */
    public HashMapDataCache(int maxRecords, long maxHoldTime, long clearInterval) {
        this.maxRecords = maxRecords;
        this.maxHoldTime = maxHoldTime;
        this.dataLock = new Object();
        this.dataMap = new HashMap<>();

        HashMapDataCache<K, T> hashMapDataCache = this;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                hashMapDataCache.clearUp();
            }
        }, clearInterval, clearInterval);
    }

    public void add(K key, @NotNull T value) {
        boolean clearUp = false;

        synchronized (this.dataLock) {
            this.dataMap.put(key, new Record<>(value, System.currentTimeMillis()));
            if (this.dataMap.size() > this.maxRecords) {
                clearUp = true;
            }
        }

        if (clearUp) {
            this.clearUpExceedingData();
        }
    }

    @Nullable
    public T get(K key) {
        synchronized (this.dataLock) {
            return this.dataMap.containsKey(key)
                    ? this.dataMap.get(key).data
                    : null;
        }
    }

    private void clearUpExceedingData() {
        synchronized (this.dataLock) {
            int toDelete = this.dataMap.size() - this.maxRecords;
            if (toDelete <= 0) return;

            Set<K> keysToRemove = this.dataMap.entrySet().stream()
                    .sorted((d1, d2) -> (int) (d1.getValue().createdAt - d2.getValue().createdAt))
                    .limit(toDelete).map(Map.Entry::getKey).collect(Collectors.toSet());

            for (K key : keysToRemove) {
                this.dataMap.remove(key);
            }
        }
    }

    private void clearUp() {
        synchronized (this.dataLock) {
            long now = System.currentTimeMillis();
            this.dataMap.values().removeIf(d -> (now - d.createdAt) > this.maxHoldTime);
        }
    }
}
