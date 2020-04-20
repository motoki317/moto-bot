package db.repository.mariadb;

import db.ConnectionPool;
import db.model.serverLog.ServerLogEntry;
import db.model.serverLog.ServerLogEntryId;
import db.repository.base.ServerLogRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MariaServerLogRepository extends ServerLogRepository {
    MariaServerLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected ServerLogEntry bind(@NotNull ResultSet res) throws SQLException {
        return new ServerLogEntry(res.getLong(1), res.getLong(2));
    }

    @Override
    public <S extends ServerLogEntry> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `server_log` (guild_id, channel_id) VALUES (?, ?)",
                entity.getGuildId(),
                entity.getChannelId()
        );
    }

    @Override
    public boolean exists(@NotNull ServerLogEntryId serverLogEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `server_log` WHERE `guild_id` = ?",
                serverLogEntryId.getGuildId()
        );

        if (res == null) {
            return false;
        }

        try {
            if (res.next())
                return res.getInt(1) > 0;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `server_log`"
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return -1;
    }

    @Nullable
    @Override
    public ServerLogEntry findOne(@NotNull ServerLogEntryId serverLogEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `server_log` WHERE `guild_id` = ?",
                serverLogEntryId.getGuildId()
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public List<ServerLogEntry> findAllIn(long... guildIDs) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `server_log` WHERE `guild_id` IN (" +
                        String.join(", ", Collections.nCopies(guildIDs.length, "?")) +
                        ")",
                Arrays.stream(guildIDs).boxed().toArray()
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Nullable
    @Override
    public List<ServerLogEntry> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `server_log`"
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public boolean update(@NotNull ServerLogEntry entity) {
        return this.execute(
                "UPDATE `server_log` SET `channel_id` = ? WHERE `guild_id` = ?",
                entity.getChannelId(),
                entity.getGuildId()
        );
    }

    @Override
    public boolean delete(@NotNull ServerLogEntryId serverLogEntryId) {
        return this.execute(
                "DELETE FROM `server_log` WHERE `guild_id` = ?",
                serverLogEntryId.getGuildId()
        );
    }
}
