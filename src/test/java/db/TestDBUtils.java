package db;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import utils.TestUtils;

import java.sql.SQLException;

class TestDBUtils {
    @NotNull
    @Contract(" -> new")
    @TestOnly
    static Database createDatabase() {
        try {
            return new DatabaseConnection(TestUtils.getLogger());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error();
        }
    }
}
