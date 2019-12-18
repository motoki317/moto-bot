package db;

import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SimpleConnectionPool implements ConnectionPool {
    private static final long MAX_RELEASE_WAIT = TimeUnit.SECONDS.toMillis(60);
    private static final long MAX_GET_WAIT = TimeUnit.SECONDS.toMillis(3);
    private static final long WAIT_INTERVAL = TimeUnit.MILLISECONDS.toMillis(250);

    private final String url;
    private final Logger logger;

    private final Object lock = new Object();
    private final Stack<Connection> availableConnections;
    private final Map<Connection, Long> usedConnectionTime;

    SimpleConnectionPool(String url, Logger logger, int maxConnections) {
        this.url = url;
        this.logger = logger;

        this.availableConnections = new Stack<>();
        for (int i = 0; i < maxConnections; i++) {
            this.availableConnections.push(null);
        }
        this.usedConnectionTime = new HashMap<>();
    }

    @NotNull
    private static Connection createConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    @Nullable
    public Connection getConnection() {
        if (availableConnections.isEmpty()) {
            long start = System.currentTimeMillis();
            while (availableConnections.isEmpty() && System.currentTimeMillis() - start < MAX_GET_WAIT) {
                try {
                    Thread.sleep(WAIT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        this.checkUnreleasedConnections();
        synchronized (this.lock) {
            if (availableConnections.isEmpty()) {
                return null;
            }

            Connection connection = availableConnections.pop();
            if (connection == null) {
                try {
                    connection = createConnection(url);
                } catch (SQLException e) {
                    availableConnections.push(null);
                    this.logger.logException("failed to establish a new db connection", e);
                    return null;
                }
            }

            this.logger.log(-1, "Created a new db connection (remaining: " + availableConnections.size() + ")");
            usedConnectionTime.put(connection, System.currentTimeMillis());
            return connection;
        }
    }

    private void checkUnreleasedConnections() {
        synchronized (this.lock) {
            long current = System.currentTimeMillis();
            Iterator<Map.Entry<Connection, Long>> it = this.usedConnectionTime.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Connection, Long> e = it.next();
                if (MAX_RELEASE_WAIT < current - e.getValue()) {
                    try {
                        availableConnections.push(createConnection(this.url));
                        it.remove();
                        this.logger.log(0,
                                String.format("Removing unreleased connection from pool, elapsed %s ms.",
                                        current - e.getValue()));
                    } catch (SQLException ex) {
                        this.logger.logException("Tried to remove unreleased connection from pool, " +
                                "an exception occurred while creating new connection.", ex);
                    }
                }
            }
        }
    }

    @Override
    public void releaseConnection(@NotNull Connection connection) {
        synchronized (this.lock) {
            if (usedConnectionTime.containsKey(connection)) {
                usedConnectionTime.remove(connection);
                availableConnections.push(connection);
                this.logger.log(-1, "Connection returned (remaining " + this.availableConnections.size() + ")");
            }
            // Else, this connection was discarded from pool by checkUnreleasedConnections()
            // because it was not released for a long time
        }
    }
}
