package db.repository.mariadb;

import db.ConnectionPool;
import db.model.commandLog.CommandLog;
import db.model.commandLog.CommandLogId;
import db.repository.base.CommandLogRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

class MariaCommandLogRepository extends CommandLogRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaCommandLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected CommandLog bind(@NotNull ResultSet res) throws SQLException {
        return new CommandLog(res.getInt(1), res.getString(2), res.getString(3),
                res.getLong(4) != 0 ? res.getLong(4) : null,
                res.getLong(5), res.getLong(6), res.getTimestamp(7));
    }

    @Override
    public <S extends CommandLog> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `command_log` (kind, full, guild_id, channel_id, user_id, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                entity.getKind(),
                entity.getFull(),
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getUserId(),
                dbFormat.format(entity.getCreatedAt())
        );
    }

    @Override
    public boolean exists(@NotNull CommandLogId commandLogId) {
        ResultSet res = this.executeQuery("SELECT COUNT(*) FROM `command_log` WHERE `id` = ?", commandLogId.getId());
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
        ResultSet res = this.executeQuery("SELECT COUNT(*) FROM `command_log`");
        if (res == null) {
            return 0;
        }

        try {
            if (res.next())
                return res.getLong(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return 0;
    }

    @Nullable
    @Override
    public CommandLog findOne(@NotNull CommandLogId commandLogId) {
        ResultSet res = this.executeQuery("SELECT * FROM `command_log` WHERE `id` = ?", commandLogId.getId());
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

    @Nullable
    @Override
    public List<CommandLog> findAll() {
        ResultSet res = this.executeQuery("SELECT * FROM `command_log`");
        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public boolean update(@NotNull CommandLog entity) {
        return this.execute(
                "UPDATE `command_log` SET `kind` = ?, `full` = ?, `guild_id` = ?, `channel_id` = ?, `user_id` = ?, `created_at` = ? WHERE `id` = ?",
                entity.getKind(),
                entity.getFull(),
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getUserId(),
                dbFormat.format(entity.getCreatedAt()),
                entity.getId()
        );
    }

    @Override
    public boolean delete(@NotNull CommandLogId commandLogId) {
        return this.execute(
                "DELETE FROM `command_log` WHERE `id` = ?",
                commandLogId.getId()
        );
    }
}
