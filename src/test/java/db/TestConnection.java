package db;

import org.junit.jupiter.api.Test;

class TestConnection {
    @Test
    void testConnection() {
        assert TestDBUtils.createConnection() != null;
    }
}
