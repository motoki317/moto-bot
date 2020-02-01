package db.repository.mariadb;

import db.ConnectionPool;
import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.model.playerWarLeaderboard.PlayerWarLeaderboardId;
import db.repository.base.PlayerWarLeaderboardRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaPlayerWarLeaderboardRepository extends PlayerWarLeaderboardRepository {
    MariaPlayerWarLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected PlayerWarLeaderboard bind(@NotNull ResultSet res) throws SQLException {
        return new PlayerWarLeaderboard(res.getString(1), res.getString(2),
                res.getInt(3), res.getInt(4), res.getInt(5),
                res.getBigDecimal(6).doubleValue(), res.getBigDecimal(7).doubleValue());
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
