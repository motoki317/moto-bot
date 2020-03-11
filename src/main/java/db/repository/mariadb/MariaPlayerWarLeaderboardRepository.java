package db.repository.mariadb;

import db.ConnectionPool;
import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.model.playerWarLeaderboard.PlayerWarLeaderboardId;
import db.repository.base.PlayerWarLeaderboardRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import utils.UUID;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

class MariaPlayerWarLeaderboardRepository extends PlayerWarLeaderboardRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaPlayerWarLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected PlayerWarLeaderboard bind(@NotNull ResultSet res) throws SQLException {
        return new PlayerWarLeaderboard(res.getString(1), res.getString(2),
                res.getInt(3), res.getInt(4), res.getInt(5),
                res.getMetaData().getColumnCount() > 5 ? res.getBigDecimal(6) : null,
                res.getMetaData().getColumnCount() > 6 ? res.getBigDecimal(7): null);
    }

    @Override
    public <S extends PlayerWarLeaderboard> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `player_war_leaderboard` (uuid, last_name, total_war, success_war, survived_war) VALUES (?, ?, ?, ? ,?)",
                entity.getUUID(),
                entity.getLastName(),
                entity.getTotalWar(),
                entity.getSuccessWar(),
                entity.getSurvivedWar()
        );
    }

    @Override
    public boolean exists(@NotNull PlayerWarLeaderboardId playerWarLeaderboardId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `player_war_leaderboard` WHERE `uuid` = ?",
                playerWarLeaderboardId.getUUID()
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
                "SELECT COUNT(*) FROM `player_war_leaderboard`"
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
    public PlayerWarLeaderboard findOne(@NotNull PlayerWarLeaderboardId playerWarLeaderboardId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_war_leaderboard` WHERE `uuid` = ?",
                playerWarLeaderboardId.getUUID()
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
    public List<PlayerWarLeaderboard> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_war_leaderboard`"
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

    @Nullable
    @Override
    public List<PlayerWarLeaderboard> getByTotalWarDescending(int limit, int offset) {
        ResultSet res = this.executeQuery(
                // secondary sort by uuid to get consistent paging result
                "SELECT * FROM `player_war_leaderboard` ORDER BY `total_war` DESC, `uuid` DESC LIMIT " + limit + " OFFSET " + offset
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
    public List<PlayerWarLeaderboard> getBySuccessWarDescending(int limit, int offset) {
        ResultSet res = this.executeQuery(
                // secondary sort by uuid to get consistent paging result
                "SELECT * FROM `player_war_leaderboard` ORDER BY `success_war` DESC, `uuid` DESC LIMIT " + limit + " OFFSET " + offset
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
    public List<PlayerWarLeaderboard> getBySurvivedWarDescending(int limit, int offset) {
        ResultSet res = this.executeQuery(
                // secondary sort by uuid to get consistent paging result
                "SELECT * FROM `player_war_leaderboard` ORDER BY `survived_war` DESC, `uuid` DESC LIMIT " + limit + " OFFSET " + offset
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
    public List<PlayerWarLeaderboard> getRecordsOf(List<UUID> playerUUIDs) {
        String placeHolder = "?";

        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_war_leaderboard` WHERE `uuid` IN ("
                        + playerUUIDs.stream().map(p -> placeHolder).collect(Collectors.joining(", "))
                        + ")",
                playerUUIDs.stream().map(UUID::toStringWithHyphens).toArray()
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
    public int getPlayersInRange(@NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return -1;
        }

        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM (" +
                        "SELECT `player_uuid` FROM `war_player` " +
                        "WHERE `war_log_id` >= ? AND `war_log_id` < ? " +
                        "GROUP BY `player_uuid` " +
                        "HAVING COUNT(*) > 0" +
                        ") AS t",
                first, last
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

    @Nullable
    @Override
    public List<PlayerWarLeaderboard> getByTotalWarDescending(int limit, int offset, @NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return null;
        }

        ResultSet res = this.executeQuery(
                "SELECT *, " +
                        "`player_success_wars_between`(player_uuid, ?, ?) AS success_wars, " +
                        // default view is total/success wars only, to reduce computation time
                        "0 AS survived_wars " +
                        "FROM (SELECT `player_uuid`, `player_name`, " +
                        "COUNT(*) AS total_wars " +
                        "FROM `war_player` " +
                        "WHERE `war_log_id` >= ? AND `war_log_id` < ? " +
                        "GROUP BY `player_uuid` " +
                        "HAVING `total_wars` > 0 AND player_uuid IS NOT NULL " +
                        "ORDER BY `total_wars` DESC, `player_uuid` DESC " +
                        "LIMIT " + limit + " OFFSET " + offset + ") AS t",
                first, last,
                first, last
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
    public List<PlayerWarLeaderboard> getBySuccessWarDescending(int limit, int offset, @NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return null;
        }

        ResultSet res = this.executeQuery(
                "SELECT t.player_uuid, t.player_name, " +
                        "`player_total_wars_between`(player_uuid, ?, ?) AS total_wars, " +
                        "t.success_wars, " +
                        // default view is total/success wars only, to reduce computation time
                        "0 AS survived_wars " +
                        "FROM (SELECT t.player_uuid, t.player_name, COUNT(*) AS `success_wars` FROM " +
                        "(SELECT * FROM `war_player` WHERE `war_log_id` >= ? AND `war_log_id` < ?) AS t " +
                        "LEFT JOIN `guild_war_log` g ON g.war_log_id = t.war_log_id AND g.territory_log_id IS NOT NULL " +
                        "GROUP BY t.`player_uuid` " +
                        "HAVING `success_wars` > 0 AND player_uuid IS NOT NULL " +
                        "ORDER BY `success_wars` DESC, `player_uuid` DESC " +
                        "LIMIT " + limit + " OFFSET " + offset + ") AS t",
                first, last,
                first, last
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
    public List<PlayerWarLeaderboard> getBySurvivedWarDescending(int limit, int offset, @NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return null;
        }

        ResultSet res = this.executeQuery(
                "SELECT t.player_uuid, t.player_name, " +
                        "`player_total_wars_between`(player_uuid, ?, ?) AS total_wars, " +
                        // default view is total/survived wars only, to reduce computation time
                        "0 AS success_wars, " +
                        "t.survived_wars " +
                        "FROM (SELECT t.player_uuid, t.player_name, COUNT(*) AS `survived_wars` FROM " +
                        "(SELECT * FROM `war_player` WHERE `war_log_id` >= ? AND `war_log_id` < ? AND NOT `exited`) AS t " +
                        "LEFT JOIN `guild_war_log` g ON g.war_log_id = t.war_log_id AND g.territory_log_id IS NOT NULL " +
                        "GROUP BY t.`player_uuid` " +
                        "HAVING `survived_wars` > 0 AND player_uuid IS NOT NULL " +
                        "ORDER BY `survived_wars` DESC, `player_uuid` DESC " +
                        "LIMIT " + limit + " OFFSET " + offset + ") AS t",
                first, last,
                first, last
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
    public List<PlayerWarLeaderboard> getRecordsOf(List<UUID> playerUUIDs, @NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return null;
        }

        String UUIDs = playerUUIDs.stream().map(p -> "\"" + p.toStringWithHyphens() + "\"").collect(Collectors.joining(", "));

        ResultSet res = this.executeQuery(
                "SELECT *, " +
                        "`player_success_wars_between`(player_uuid, ?, ?) AS success_wars, " +
                        "`player_survived_wars_between`(player_uuid, ?, ?) AS survived_wars " +
                        "FROM (SELECT `player_uuid`, `player_name`, " +
                        "COUNT(*) AS total_wars " +
                        "FROM `war_player` " +
                        "WHERE `war_log_id` >= ? AND `war_log_id` < ? " +
                        "GROUP BY `player_uuid` " +
                        "HAVING `total_wars` > 0 AND player_uuid IN (" + UUIDs + ")) AS t",
                first, last,
                first, last,
                first, last
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
    public boolean update(@NotNull PlayerWarLeaderboard entity) {
        return this.execute(
                "UPDATE `player_war_leaderboard` SET `last_name` = ?, `total_war` = ?, `success_war` = ?, `survived_war` = ? WHERE `uuid` = ?",
                entity.getLastName(),
                entity.getTotalWar(),
                entity.getSuccessWar(),
                entity.getSurvivedWar(),
                entity.getUUID()
        );
    }

    @Override
    public boolean delete(@NotNull PlayerWarLeaderboardId playerWarLeaderboardId) {
        return this.execute(
                "DELETE FROM `player_war_leaderboard` WHERE `uuid` = ?",
                playerWarLeaderboardId.getUUID()
        );
    }
}
