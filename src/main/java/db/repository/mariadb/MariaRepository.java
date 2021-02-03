package db.repository.mariadb;

import db.ConnectionPool;
import log.Logger;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class MariaRepository<T> {
    protected final ConnectionPool db;

    protected final Logger logger;

    protected MariaRepository(ConnectionPool db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    private PreparedStatement prepareStatement(@NotNull Connection connection,
                                               @NotNull String sql,
                                               Object[] objects) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                ps.setObject(i + 1, objects[i]);
            } else {
                ps.setString(i + 1, objects[i].toString());
            }
        }
        return ps;
    }

    /**
     * Executes sql statement and handles exceptions.
     * Automatically handles connections.
     * @param sql any SQL statement
     * @return True if succeeded.
     */
    @CheckReturnValue
    protected boolean execute(@Language("MariaDB") String sql, Object... strings) {
        Connection connection = this.db.getConnection();
        if (connection == null) {
            return false;
        }

        try {
            return execute(connection, sql, strings);
        } finally {
            this.db.releaseConnection(connection);
        }
    }

    /**
     * Executes sql statement with given connection, and handles exceptions.
     * This method will NOT release the given connection.
     * @param sql any SQL statement
     * @param connection Connection to use.
     * @return True if succeeded.
     */
    @CheckReturnValue
    protected boolean execute(Connection connection, @Language("MariaDB") String sql, Object... objects) {
        try {
            PreparedStatement ps = this.prepareStatement(connection, sql, objects);
            ps.execute();
            return true;
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while executing sql: " + sql + ", parameters: " + Arrays.toString(objects), e);
            return false;
        }
    }

    /**
     * Executes sql statement and handles exceptions.
     * Automatically handles connections.
     * @param sql any SQL statement
     * @param objects objects to replace "?" in the query
     * @return True if succeeded.
     */
    @Nullable
    @CheckReturnValue
    protected ResultSet executeQuery(@Language("MariaDB") String sql, Object... objects) {
        Connection connection = this.db.getConnection();
        if (connection == null) {
            return null;
        }

        try {
            return executeQuery(connection, sql, objects);
        } finally {
            this.db.releaseConnection(connection);
        }
    }

    /**
     * Executes sql statement with given connection, and handles exceptions.
     * This method will NOT release the given connection.
     * @param sql any SQL statement
     * @param connection Connection to use.
     * @param objects objects to replace "?" in the query
     * @return True if succeeded.
     */
    @Nullable
    @CheckReturnValue
    protected ResultSet executeQuery(Connection connection, @Language("MariaDB") String sql, Object... objects) {
        try {
            PreparedStatement ps = this.prepareStatement(connection, sql, objects);
            return ps.executeQuery();
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while executing sql: " + sql + ", parameters: " + Arrays.toString(objects), e);
            return null;
        }
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
    @NotNull
    protected List<T> bindAll(@NotNull ResultSet res) throws SQLException {
        List<T> ret = new ArrayList<>();
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
