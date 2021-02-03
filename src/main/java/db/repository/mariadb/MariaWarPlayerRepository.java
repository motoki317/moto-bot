package db.repository.mariadb;

import db.ConnectionPool;
import db.model.warPlayer.WarPlayer;
import db.model.warPlayer.WarPlayerId;
import db.repository.base.WarPlayerRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import utils.UUID;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class MariaWarPlayerRepository extends MariaRepository<WarPlayer> implements WarPlayerRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaWarPlayerRepository(ConnectionPool db, Logger logger) {
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

    /**
     * Creates entity using the given connection.
     * @return {@code true} if success.
     */
    <S extends WarPlayer> boolean create(@NotNull Connection connection, @NotNull S entity) {
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
    public List<WarPlayer> findAllOfWarLogId(int warLogId) {
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

    public int countOfPlayer(UUID playerUUID) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_player` WHERE `player_uuid` = ?",
                playerUUID.toStringWithHyphens()
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

    @Override
    public int countOfPlayer(UUID playerUUID, String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) AS `total_wars` FROM `guild_war_log` g " +
                        "JOIN `war_player` wp " +
                        "ON g.guild_name = ? " +
                        "AND g.war_log_id = wp.war_log_id " +
                        "AND wp.player_uuid = ?",
                guildName,
                playerUUID.toStringWithHyphens()
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

    public int countSuccessWars(UUID playerUUID) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_player` p JOIN `guild_war_log` gwl ON p.player_uuid = ?" +
                        " AND p.war_log_id = gwl.war_log_id WHERE gwl.territory_log_id IS NOT NULL",
                playerUUID.toStringWithHyphens()
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

    @Override
    public int countSuccessWars(UUID playerUUID, String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT SUM(g.territory_log_id IS NOT NULL) AS `total_wars` FROM `guild_war_log` g " +
                        "JOIN `war_player` wp " +
                        "ON g.guild_name = ? " +
                        "AND g.war_log_id = wp.war_log_id " +
                        "AND wp.player_uuid = ?",
                guildName,
                playerUUID.toStringWithHyphens()
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

    public int countSurvivedWars(UUID playerUUID) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_player` p JOIN `guild_war_log` gwl ON p.player_uuid = ?" +
                        " AND p.exited = 0 AND p.war_log_id = gwl.war_log_id WHERE gwl.territory_log_id IS NOT NULL",
                playerUUID.toStringWithHyphens()
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

    @Override
    public int countSurvivedWars(UUID playerUUID, String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT SUM(g.territory_log_id IS NOT NULL AND NOT wp.exited) AS `total_wars` FROM `guild_war_log` g " +
                        "JOIN `war_player` wp " +
                        "ON g.guild_name = ? " +
                        "AND g.war_log_id = wp.war_log_id " +
                        "AND wp.player_uuid = ?",
                guildName,
                playerUUID.toStringWithHyphens()
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
    public List<WarPlayer> getLogsOfPlayer(UUID playerUUID, int limit, int offset) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_player` WHERE `player_uuid` = ? ORDER BY `war_log_id` DESC LIMIT " + limit + " OFFSET " + offset,
                playerUUID.toStringWithHyphens()
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
    public List<WarPlayer> getLogsOfPlayer(UUID playerUUID, String guildName, int limit, int offset) {
        ResultSet res = this.executeQuery(
                "SELECT wp.* FROM `guild_war_log` g " +
                        "JOIN `war_player` wp " +
                        "ON g.guild_name = ? " +
                        "AND g.war_log_id = wp.war_log_id " +
                        "AND wp.player_uuid = ? " +
                        "ORDER BY g.id DESC LIMIT " + limit + " OFFSET " + offset,
                guildName,
                playerUUID.toStringWithHyphens()
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
    public WarPlayer getUUIDNullPlayer(int offset) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_player` WHERE `player_uuid` IS NULL ORDER BY `war_log_id` LIMIT 1 OFFSET " + offset
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

    private int getFirstWarLogIdAfter(@NotNull Date date) {
        ResultSet res = this.executeQuery(
                "SELECT first_war_log_id_after(?)",
                dbFormat.format(date)
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

    @Override
    public boolean updatePlayerUUIDBetween(String playerName, UUID uuid, Date start, Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return false;
        }

        return this.execute(
                "UPDATE `war_player` SET `player_uuid` = ? WHERE `player_name` = ? AND `war_log_id` >= ? AND `war_log_id` < ?",
                uuid.toStringWithHyphens(), playerName, first, last
        );
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

    /**
     * Updates entity using the given connection.
     * @return {@code true} if success.
     */
    boolean update(@NotNull Connection connection, @NotNull WarPlayer entity) {
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
