package db.repository.base;

import log.Logger;
import org.intellij.lang.annotations.Language;
import org.mariadb.jdbc.internal.util.Utils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class Repository<T, ID> implements IRepository<T, ID> {
    protected final Connection db;

    protected final Logger logger;

    protected Repository(Connection db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    /**
     * Executes sql statement and handles error.
     * @param sql any SQL statement
     */
    protected void execute(@Language("MariaDB") String sql, Object... strings) {
        String fullSql = replaceSql(sql, strings);
        try {
            Statement statement = this.db.createStatement();
            statement.execute(fullSql);
        } catch (SQLException e) {
            this.logger.logError("an error occurred while executing sql: " + fullSql, e);
        }
    }

    @Nullable
    @CheckReturnValue
    protected ResultSet executeQuery(@Language("MariaDB") String sql, Object... strings) {
        String fullSql = replaceSql(sql, strings);
        try {
            Statement statement = this.db.createStatement();
            return statement.executeQuery(fullSql);
        } catch (SQLException e) {
            this.logger.logError("an error occurred while executing sql: " + fullSql, e);
            return null;
        }
    }

    private static String replaceSql(String sql, Object... strings) {
        return String.format(
                sql.replaceAll("\\?", "\"%s\""),
                (Object[]) escapeStrings(strings)
        );
    }

    private static String[] escapeStrings(Object[] strings) {
        return Arrays.stream(strings)
                .map(Object::toString)
                .map(s -> Utils.escapeString(s, true))
                .collect(Collectors.toList())
                .toArray(new String[]{});
    }

    protected void logResponseError(SQLException e) {
        this.logger.logError("an error occurred while reading from db response", e);
    }
}
