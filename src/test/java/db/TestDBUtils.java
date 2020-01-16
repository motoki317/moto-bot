package db;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import utils.TestUtils;

class TestDBUtils {
    @NotNull
    @Contract(" -> new")
    @TestOnly
    static Database createDatabase() {
        return new DatabaseImpl(TestUtils.getLogger());
    }

    private static final String MYSQL_HOST = System.getenv("MYSQL_HOST");
    private static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
    private static final String MYSQL_USER = System.getenv("MYSQL_USER");
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final int MYSQL_PORT = Integer.parseInt(System.getenv("MYSQL_PORT"));

    private static final String URL =  String.format(
            "jdbc:mariadb://%s:%s/%s?user=%s&password=%s",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD);

    @NotNull
    @Contract(" -> new")
    @TestOnly
    static ConnectionPool createConnection() {
        return new SimpleConnectionPool(URL, TestUtils.getLogger(), 10);
    }
}
