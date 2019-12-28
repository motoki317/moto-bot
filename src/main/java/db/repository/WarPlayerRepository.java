package db.repository;

import db.ConnectionPool;
import db.model.warPlayer.WarPlayer;
import db.model.warPlayer.WarPlayerId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WarPlayerRepository extends Repository<WarPlayer, WarPlayerId> {
    public WarPlayerRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected WarPlayer bind(@NotNull ResultSet res) throws SQLException {
        return new WarPlayer(res.getInt(1), res.getString(2), res.getString(3), res.getBoolean(4));
    }

    @Override
    public <S extends WarPlayer> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `war_player` (war_log_id, player_name, player_uuid, exited) VALUES (?, ?, ?, ?)",
                entity.getWarLogId(),
                entity.getPlayerName(),
                entity.getPlayerUUID(),
                entity.hasExited() ? 1 : 0
        );
    }

    public <S extends WarPlayer> boolean create(@NotNull Connection connection, @NotNull S entity) {
        return this.execute(connection,
                "INSERT INTO `war_player` (war_log_id, player_name, player_uuid, exited) VALUES (?, ?, ?, ?)",
                entity.getWarLogId(),
                entity.getPlayerName(),
                entity.getPlayerUUID(),
                entity.hasExited() ? 1 : 0
        );
    }

    @Override
    public boolean exists(@NotNull WarPlayerId warPlayerId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_player` WHERE `war_log_id` = ? AND `player_name` = ?",
                warPlayerId.getWarLogId(),
                warPlayerId.getPlayerName()
        );

        if (res == null) {
            return false;
        }

        try {
            if (res.next())
                return res.getLong(1) > 0;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_player`"
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next())
                return res.getLong(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return -1;
    }

    @Nullable
    @Override
    public WarPlayer findOne(@NotNull WarPlayerId warPlayerId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_player` WHERE `war_log_id` = ? AND `player_name` = ?",
                warPlayerId.getWarLogId(),
                warPlayerId.getPlayerName()
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

    @Nullable
    @Override
    public List<WarPlayer> findAll() {
        throw new Error("Find all not implemented: number of records may be huge, use limit, offset, where clauses instead.");
    }

    @Nullable
    List<WarPlayer> findAllOfWarLogId(int warLogId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_player` WHERE `war_log_id` = ?",
                warLogId
        );

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
    public boolean update(@NotNull WarPlayer entity) {
        return this.execute(
                "UPDATE `war_player` SET `player_uuid` = ?, `exited` = ? WHERE `war_log_id` = ? AND `player_name` = ?",
                entity.getPlayerUUID(),
                entity.hasExited() ? 1 : 0,
                entity.getWarLogId(),
                entity.getPlayerName()
        );
    }

    public boolean update(@NotNull Connection connection, @NotNull WarPlayer entity) {
        return this.execute(connection,
                "UPDATE `war_player` SET `player_uuid` = ?, `exited` = ? WHERE `war_log_id` = ? AND `player_name` = ?",
                entity.getPlayerUUID(),
                entity.hasExited() ? 1 : 0,
                entity.getWarLogId(),
                entity.getPlayerName()
        );
    }

    @Override
    public boolean delete(@NotNull WarPlayerId warPlayerId) {
        throw new Error("Delete not implemented: unintended behavior for this table");
    }
}
