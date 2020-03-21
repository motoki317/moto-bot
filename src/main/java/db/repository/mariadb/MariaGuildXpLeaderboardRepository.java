package db.repository.mariadb;

import db.ConnectionPool;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.guildXpLeaderboard.GuildXpLeaderboardId;
import db.repository.base.GuildXpLeaderboardRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class MariaGuildXpLeaderboardRepository extends GuildXpLeaderboardRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaGuildXpLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected GuildXpLeaderboard bind(@NotNull ResultSet res) throws SQLException {
        return new GuildXpLeaderboard(
                res.getString(1), res.getString(2), res.getInt(3),
                res.getLong(4), res.getLong(5),
                res.getTimestamp(6), res.getTimestamp(7)
        );
    }

    @Override
    public <S extends GuildXpLeaderboard> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `guild_xp_leaderboard` (name, prefix, level, xp, xp_diff, `from`, `to`) VALUES (?, ?, ?, ?, ?, ?, ?)",
                entity.getName(),
                entity.getPrefix(),
                entity.getLevel(),
                entity.getXp(),
                entity.getXpDiff(),
                dbFormat.format(entity.getFrom()),
                dbFormat.format(entity.getTo())
        );
    }

    public boolean createAll(@NotNull List<GuildXpLeaderboard> list) {
        if (list.size() == 0) return true;

        String singlePlaceHolder = "(?, ?, ?, ?, ?, ?, ?)";
        String placeHolders = list.stream().map(g -> singlePlaceHolder).collect(Collectors.joining(", "));
        Object[] objects = list.stream()
                .map(g -> new Object[]{
                        g.getName(),
                        g.getPrefix(),
                        g.getLevel(),
                        g.getXp(),
                        g.getXpDiff(),
                        dbFormat.format(g.getFrom()),
                        dbFormat.format(g.getTo())
                })
                .flatMap(Arrays::stream).toArray();
        return this.execute(
                "INSERT INTO `guild_xp_leaderboard` (name, prefix, level, xp, xp_diff, `from`, `to`) VALUES " + placeHolders,
                objects
        );
    }

    @Override
    public int getRank(@NotNull String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT t.`rank` FROM (SELECT `name`, RANK() OVER (ORDER BY `level` DESC, `xp` DESC) AS `rank` FROM `guild_xp_leaderboard`) AS t WHERE t.`name` = ?",
                guildName
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
    public boolean exists(@NotNull GuildXpLeaderboardId guildXpLeaderboardId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_xp_leaderboard` WHERE `name` = ?",
                guildXpLeaderboardId.getName()
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
                "SELECT COUNT(*) FROM `guild_xp_leaderboard`"
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
    public GuildXpLeaderboard findOne(@NotNull GuildXpLeaderboardId guildXpLeaderboardId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_xp_leaderboard` WHERE `name` = ?",
                guildXpLeaderboardId.getName()
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
    public List<GuildXpLeaderboard> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_xp_leaderboard`"
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
    public boolean update(@NotNull GuildXpLeaderboard entity) {
        return this.execute(
                "UPDATE `guild_xp_leaderboard` SET `prefix` = ?, `level` = ?, `xp` = ?, `xp_diff` = ?, `from` = ?, `to` = ? WHERE `name` = ?",
                entity.getPrefix(),
                entity.getLevel(),
                entity.getXp(),
                entity.getXpDiff(),
                dbFormat.format(entity.getFrom()),
                dbFormat.format(entity.getTo()),
                entity.getName()
        );
    }

    @Override
    public boolean delete(@NotNull GuildXpLeaderboardId guildXpLeaderboardId) {
        return this.execute(
                "DELETE FROM `guild_xp_leaderboard` WHERE `name` = ?",
                guildXpLeaderboardId.getName()
        );
    }

    public boolean truncateTable() {
        return this.execute(
                "TRUNCATE TABLE `guild_xp_leaderboard`"
        );
    }
}
