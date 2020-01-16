package db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

class TestConnection {
    @Test
    void testConnection() throws SQLException {
        ConnectionPool pool = TestDBUtils.createConnection();
        Connection connection = pool.getConnection();
        assert connection != null;
        assert pool.getConnection().isValid(1);
    }
}
