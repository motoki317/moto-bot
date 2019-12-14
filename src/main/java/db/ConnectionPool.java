package db;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Connection;

public interface ConnectionPool {
    @Nullable
    Connection getConnection();
    void releaseConnection(@NotNull Connection connection);
}
