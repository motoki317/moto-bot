package db.repository.base;

import log.Logger;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.mariadb.jdbc.internal.util.Utils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class Repository<T, ID> implements IRepository<T, ID> {
    protected final Connection db;

    protected final Logger logger;

    protected Repository(Connection db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    /**
     * Executes sql statement and handles exceptions.
     * @param sql any SQL statement
     * @return True if succeeded.
     */
    protected boolean execute(@Language("MariaDB") String sql, Object... strings) {
        String fullSql = replaceSql(sql, strings);
        try {
            Statement statement = this.db.createStatement();
            statement.execute(fullSql);
            return true;
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while executing sql: " + fullSql, e);
            return false;
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
            this.logger.logException("an exception occurred while executing sql: " + fullSql, e);
            return null;
        }
    }

    private static String replaceSql(String sql, @NotNull Object... strings) {
        String ret = sql;
        for (Object o : strings) {
            ret = ret.replaceFirst("\\?", escapeString(o));
        }
        return ret;
    }

    @NotNull
    private static String escapeString(@Nullable Object o) {
        return o == null ? "NULL" : "\"" + Utils.escapeString(o.toString(), true) + "\"";
    }

    protected void logResponseException(SQLException e) {
        this.logger.logException("an exception occurred while reading from db response", e);
    }

    /**
     * Binds result to list of instance from ResultSet of `SELECT * ...` query.
     * @param res Result set.
     * @return An instance.
     * @throws SQLException on read exception.
     */
    protected List<T> bindAll(@Nullable ResultSet res) throws SQLException {
        List<T> ret = new ArrayList<>();

        if (res == null) return ret;

        while (res.next()) {
            ret.add(bind(res));
        }
        return ret;
    }

    /**
     * Binds result to instance from ResultSet of `SELECT * ...` query.
     * @param res Result set.
     * @return An instance.
     * @throws SQLException on read exception.
     */
    protected abstract T bind(@NotNull ResultSet res) throws SQLException;
}
