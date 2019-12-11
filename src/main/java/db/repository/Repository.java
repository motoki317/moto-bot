package db.repository;

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

public abstract class Repository<T> implements IRepository<T> {
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
        try {
            Statement statement = this.db.createStatement();
            String fullSql = String.format(
                    sql.replaceAll("\\?", "%s"),
                    (Object[]) escapeStrings(strings)
            );
            statement.execute(fullSql);
        } catch (SQLException e) {
            this.logger.logError("an error occurred while executing sql: " + sql, e);
        }
    }

    @Nullable
    @CheckReturnValue
    protected ResultSet executeQuery(@Language("MariaDB") String sql, Object... strings) {
        try {
            Statement statement = this.db.createStatement();
            String fullSql = String.format(
                    sql.replaceAll("\\?", "%s"),
                    (Object[]) escapeStrings(strings)
            );
            return statement.executeQuery(fullSql);
        } catch (SQLException e) {
            this.logger.logError("an error occurred while executing sql: " + sql, e);
            return null;
        }
    }

    private static String[] escapeStrings(Object[] strings) {
        return Arrays.stream(strings)
                .map(Object::toString)
                .map(s -> Utils.escapeString(s, true))
                .collect(Collectors.toList())
                .toArray(new String[]{});
    }
}
