package utils.cache;

import org.junit.jupiter.api.Test;

class TestHashMapDataCache {
    @Test
    void testDataCache() {
        DataCache<String, String> cache = new HashMapDataCache<>(
                3,
                25,
                50
        );

        cache.add("k1", "data1");
        cache.add("k2", "data2");
        cache.add("k3", "data3");

        assert cache.get("k1") != null && "data1".equals(cache.get("k1"));
        assert cache.get("k2") != null && "data2".equals(cache.get("k2"));
        assert cache.get("k3") != null && "data3".equals(cache.get("k3"));

        assert cache.get("non-existent-key") == null;

        cache.add("k4", "data4");

        assert cache.get("k1") == null;
        assert cache.get("k2") != null && "data2".equals(cache.get("k2"));
        assert cache.get("k3") != null && "data3".equals(cache.get("k3"));
        assert cache.get("k4") != null && "data4".equals(cache.get("k4"));

        try {
            Thread.sleep(75);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assert cache.get("k1") == null;
        assert cache.get("k2") == null;
        assert cache.get("k3") == null;
        assert cache.get("k4") == null;
    }
}
