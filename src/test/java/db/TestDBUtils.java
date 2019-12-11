package db;

import org.jetbrains.annotations.TestOnly;
import utils.TestUtils;

import java.sql.SQLException;

class TestDBUtils {
    @TestOnly
    static DBConnection createConnection() {
        try {
            return new DBConnection(TestUtils.getLogger());
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
