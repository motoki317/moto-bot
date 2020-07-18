package db;

import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class SimpleConnectionPool implements ConnectionPool {
    private static final long MAX_RELEASE_WAIT = TimeUnit.SECONDS.toMillis(60);
    private static final long RELEASE_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(3);

    private final String url;
    private final Logger logger;

    private final int maxConnections;
    private final BlockingQueue<@NotNull Connection> availableConnections;
    // List of connections waiting to be released
    private final Object usedConnectionLock;
    private final Map<@NotNull Connection, Long> usedConnectionTime;

    public SimpleConnectionPool(String url, Logger logger, int maxConnections) {
        this.url = url;
        this.logger = logger;

        this.maxConnections = maxConnections;
        this.availableConnections = new ArrayBlockingQueue<>(this.maxConnections, true);
        this.usedConnectionLock = new Object();
        this.usedConnectionTime = new HashMap<>();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SimpleConnectionPool.this.checkUnreleasedConnections();
            }
        }, RELEASE_CHECK_INTERVAL, RELEASE_CHECK_INTERVAL);
    }

    @NotNull
    private static Connection createConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }

    private boolean isValid(Connection conn) {
        try {
            return conn.isValid(3);
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while checking validity of the connection", e);
        }
        return false;
    }

    private void close(Connection conn) {
        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while checking a connection is closed / closing a connection", e);
        }
    }

    @Override
    @Nullable
    public synchronized Connection getConnection() {
        Connection conn = null;
        // Either there are connections available, or all connections in the pool are currently used
        // then retrieve a connection from the pool
        if (!this.availableConnections.isEmpty() || this.usedConnectionTime.size() >= this.maxConnections) {
            try {
                conn = this.availableConnections.poll(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (conn == null) {
                this.logger.log(0, "Connection Pool: Failed to retrieve connection");
                return null;
            }
            // Check validity of the retrieved connection
            if (!isValid(conn)) {
                // If not valid, abort, and get a new connection
                close(conn);
                conn = null;
            }
        }

        // If there are no available connections, and the open connections are not at max
        // then open a new connection
        if (conn == null) {
            try {
                conn = createConnection(this.url);
            } catch (SQLException e) {
                this.logger.logException("Something went wrong while opening a connection to DB", e);
                return null;
            }
        }

        synchronized (this.usedConnectionLock) {
            this.usedConnectionTime.put(conn, System.currentTimeMillis());
        }
        return conn;
    }

    /**
     * Checks connection release timeout.
     * If a connection hasn't been released via {@link ConnectionPool#releaseConnection(Connection)} after a certain
     * amount of time, then abort that connection, create a new connection and add to the pool.
     */
    private void checkUnreleasedConnections() {
        synchronized (this.usedConnectionLock) {
            long current = System.currentTimeMillis();
            boolean res = this.usedConnectionTime.entrySet().removeIf(e -> MAX_RELEASE_WAIT < current - e.getValue());
            if (res) {
                this.logger.log(0, "Removing unreleased connection(s) from pool");
            }
        }
    }

    @Override
    public void releaseConnection(@NotNull Connection connection) {
        synchronized (this.usedConnectionLock) {
            if (usedConnectionTime.containsKey(connection)) {
                usedConnectionTime.remove(connection);
                availableConnections.offer(connection);
            } else {
                // Else, this connection was discarded from pool by checkUnreleasedConnections()
                // because it was not released for a long time
                close(connection);
            }
        }
    }
}
