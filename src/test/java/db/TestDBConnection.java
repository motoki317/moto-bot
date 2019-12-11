package db;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.sql.SQLException;

public class TestDBConnection {
    @TestOnly
    private static DBConnection createConnection() {
        try {
            return new DBConnection(TestUtils.getLogger());
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @Test
    void testConnection() {
        assert createConnection() != null;
    }
}
