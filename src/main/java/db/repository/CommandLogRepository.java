package db.repository;

import db.ConnectionPool;
import db.model.commandLog.CommandLog;
import db.model.commandLog.CommandLogId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CommandLogRepository extends Repository<CommandLog, CommandLogId> {
    public CommandLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected CommandLog bind(@NotNull ResultSet res) throws SQLException {
        CommandLog instance = new CommandLog(res.getString(2), res.getString(3), res.getLong(4), res.getBoolean(5));
        instance.setId(res.getInt(1));
        return instance;
    }

    @Override
    public <S extends CommandLog> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `command_log` (`kind`, `full`, `user_id`, `dm`) VALUES (?, ?, ?, ?)",
                entity.getKind(),
                entity.getFull(),
                entity.getUserId(),
                entity.isDm() ? 1 : 0
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
                "UPDATE `command_log` SET `kind` = ?, `full` = ?, `user_id` = ?, `dm` = ? WHERE `id` = ?",
                entity.getKind(),
                entity.getFull(),
                entity.getUserId(),
                entity.isDm() ? 1 : 0,
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
