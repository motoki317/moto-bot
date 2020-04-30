package db.repository.mariadb;

import db.ConnectionPool;
import db.model.guildLeaderboard.GuildLeaderboard;
import db.model.guildLeaderboard.GuildLeaderboardId;
import db.repository.base.GuildLeaderboardRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

class MariaGuildLeaderboardRepository extends GuildLeaderboardRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaGuildLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected GuildLeaderboard bind(@NotNull ResultSet res) throws SQLException {
        return new GuildLeaderboard(res.getString(1), res.getString(2),
                res.getLong(3), res.getInt(4), res.getInt(5),
                res.getInt(6), res.getInt(7), res.getTimestamp(8));
    }

    @Override
    public <S extends GuildLeaderboard> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `guild_leaderboard` (name, prefix, xp, level, num, territories, member_count, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getName(),
                entity.getPrefix(),
                entity.getXp(),
                entity.getLevel(),
                entity.getNum(),
                entity.getTerritories(),
                entity.getMemberCount(),
                dbFormat.format(entity.getUpdatedAt())
        );
    }

    public boolean createAll(@NotNull List<GuildLeaderboard> list) {
        if (list.size() == 0) return true;

        String singlePlaceHolder = "(?, ?, ?, ?, ?, ?, ?, ?)";
        String placeHolders = list.stream().map(g -> singlePlaceHolder).collect(Collectors.joining(", "));
        Object[] objects = list.stream()
                .map(g -> new Object[]{
                        g.getName(),
                        g.getPrefix(),
                        g.getXp(),
                        g.getLevel(),
                        g.getNum(),
                        g.getTerritories(),
                        g.getMemberCount(),
                        dbFormat.format(g.getUpdatedAt())
                })
                .flatMap(Arrays::stream).toArray();
        return this.execute(
                "INSERT INTO `guild_leaderboard` (name, prefix, xp, level, num, territories, member_count, updated_at) VALUES " + placeHolders,
                objects
        );
    }

    @Override
    public boolean exists(@NotNull GuildLeaderboardId guildLeaderboardId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_leaderboard` WHERE `updated_at` = ? AND `name` = ?",
                dbFormat.format(guildLeaderboardId.getUpdatedAt()),
                guildLeaderboardId.getName()
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
                "SELECT COUNT(*) FROM `guild_leaderboard`"
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
    public GuildLeaderboard findOne(@NotNull GuildLeaderboardId guildLeaderboardId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_leaderboard` WHERE `updated_at` = ? AND `name` = ?",
                dbFormat.format(guildLeaderboardId.getUpdatedAt()),
                guildLeaderboardId.getName()
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
    public List<GuildLeaderboard> getLatestLeaderboard() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_leaderboard` WHERE `updated_at` = (SELECT MAX(`updated_at`) FROM `guild_leaderboard`)"
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
    public Date getNewestDate() {
        ResultSet res = this.executeQuery(
                "SELECT MAX(`updated_at`) FROM `guild_leaderboard`"
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return res.getTimestamp(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    public Date getOldestDate() {
        ResultSet res = this.executeQuery(
                "SELECT MIN(`updated_at`) FROM `guild_leaderboard`"
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return res.getTimestamp(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    public Date getNewestDateBetween(@NotNull Date old, @NotNull Date newer) {
        ResultSet res = this.executeQuery(
                "SELECT MAX(`updated_at`) FROM `guild_leaderboard` WHERE `updated_at` > ? AND `updated_at` < ?",
                dbFormat.format(old),
                dbFormat.format(newer)
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return res.getTimestamp(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    private GuildLeaderboard getLevelRankThresholdEntry() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM guild_leaderboard WHERE `updated_at` = (SELECT MAX(`updated_at`) FROM `guild_leaderboard`) AND `territories` = 0 ORDER BY level, xp LIMIT 1"
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
    public int getLevelRank(String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT t.name, prefix, xp, level, num, territories, member_count, updated_at, `rank` FROM " +
                        "(SELECT *, RANK() OVER " +
                        "(ORDER BY `level` DESC, `xp` DESC)" +
                        " AS `rank` FROM `guild_leaderboard` WHERE `updated_at` = " +
                        "(SELECT MAX(`updated_at`) FROM `guild_leaderboard`)) AS t WHERE t.`name` = ?",
                guildName
        );

        if (res == null) {
            return -1;
        }

        try {
            if (!res.next()) {
                return -1;
            }
            int rank = res.getInt(res.findColumn("rank"));
            GuildLeaderboard entry = bind(res);
            // It is possible that the threshold doesn't exist (every guild in the LB has at least 1 territory, although extremely unlikely)
            GuildLeaderboard threshold = getLevelRankThresholdEntry();
            // The entry is lower the level rank threshold, we cannot safely return the level rank
            if (threshold != null && entry.compareLevelAndXP(threshold) < 0) {
                return -1;
            }
            return rank;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return -1;
    }

    @Nullable
    @Override
    public List<GuildLeaderboard> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_leaderboard`"
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
    public boolean update(@NotNull GuildLeaderboard entity) {
        return this.execute(
                "UPDATE `guild_leaderboard` SET `prefix` = ?, `xp` = ?, `level` = ?, `num` = ?, `territories` = ?, `member_count` = ? WHERE `updated_at` = ? AND `name` = ?",
                entity.getPrefix(),
                entity.getXp(),
                entity.getLevel(),
                entity.getNum(),
                entity.getTerritories(),
                entity.getMemberCount(),
                dbFormat.format(entity.getUpdatedAt()),
                entity.getName()
        );
    }

    @Override
    public boolean delete(@NotNull GuildLeaderboardId guildLeaderboardId) {
        return this.execute(
                "DELETE FROM `guild_leaderboard` WHERE `updated_at` = ? AND `name` = ?",
                dbFormat.format(guildLeaderboardId.getUpdatedAt()),
                guildLeaderboardId.getName()
        );
    }

    public boolean deleteAllOlderThan(@NotNull Date date) {
        return this.execute(
                "DELETE FROM `guild_leaderboard` WHERE `updated_at` < ?",
                dbFormat.format(date)
        );
    }
}
