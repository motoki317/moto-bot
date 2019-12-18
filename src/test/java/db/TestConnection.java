package db;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.sql.Connection;
import java.sql.SQLException;

class TestConnection {
    private static final String MYSQL_HOST = System.getenv("MYSQL_HOST");
    private static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
    private static final String MYSQL_USER = System.getenv("MYSQL_USER");
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final int MYSQL_PORT = Integer.parseInt(System.getenv("MYSQL_PORT"));

    private static final String URL =  String.format(
            "jdbc:mariadb://%s:%s/%s?user=%s&password=%s",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD);

    @TestOnly
    private static ConnectionPool createConnection() {
        return new SimpleConnectionPool(URL, TestUtils.getLogger(), 10);
    }

    @Test
    void testConnection() throws SQLException {
        ConnectionPool pool = createConnection();
        Connection connection = pool.getConnection();
        assert connection != null;
        assert pool.getConnection().isValid(1);
    }
}
