package db;

import log.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String MYSQL_HOST = System.getenv("MYSQL_HOST");
    private static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
    private static final String MYSQL_USER = System.getenv("MYSQL_USER");
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final int MYSQL_PORT = Integer.parseInt(System.getenv("MYSQL_PORT"));

    private final Connection connection;

    private final Logger logger;

    public DBConnection(Logger logger) throws SQLException {
        this.logger = logger;

        String url = String.format(
                "jdbc:mariadb://%s:%s/%s?user=%s&password=%s",
                MYSQL_HOST,
                MYSQL_PORT,
                MYSQL_DATABASE,
                MYSQL_USER,
                MYSQL_PASSWORD
        );
        this.logger.log(-1, "Connecting to " + url);
        this.connection = DriverManager.getConnection(url);
        this.logger.log(-1, "Successfully connected to db!");
    }
}
