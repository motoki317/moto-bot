package db.repository.mariadb;

import db.ConnectionPool;
import db.model.guildWarLog.GuildWarLog;
import db.model.guildWarLog.GuildWarLogId;
import db.repository.base.GuildWarLogRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

class MariaGuildWarLogRepository extends MariaRepository<GuildWarLog> implements GuildWarLogRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaGuildWarLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected GuildWarLog bind(@NotNull ResultSet res) throws SQLException {
        return new GuildWarLog(res.getInt(1), res.getString(2),
                res.getInt(3) != 0 ? res.getInt(3) : null,
                res.getInt(4) != 0 ? res.getInt(4) : null);
    }

    @Override
    public <S extends GuildWarLog> boolean create(@NotNull S entity) {
        throw new Error("Create not implemented: records are automatically added via triggers");
    }

    @Override
    public boolean exists(@NotNull GuildWarLogId guildWarLogId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `id` = ?",
                guildWarLogId.getId()
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
                "SELECT COUNT(*) FROM `guild_war_log`"
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
    public GuildWarLog findOne(@NotNull GuildWarLogId guildWarLogId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_war_log` WHERE `id` = ?"
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

    public int countGuildLogs(String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `guild_name` = ?",
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

    @Nullable
    public List<GuildWarLog> findGuildLogs(String guildName, int limit, int offset) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_war_log` WHERE `guild_name` = ? ORDER BY `id` DESC LIMIT " + limit + " OFFSET " + offset,
                guildName
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

    public int countSuccessWars(String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `guild_name` = ? AND `war_log_id` IS NOT NULL AND `territory_log_id` IS NOT NULL",
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

    public int countTotalWars(String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `guild_name` = ? AND `war_log_id` IS NOT NULL",
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
    public int countSuccessWars(String guildName, @NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return -1;
        }

        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `guild_name` = ? " +
                        "AND `war_log_id` >= ? AND `war_log_id` < ? AND `territory_log_id` IS NOT NULL",
                guildName, first, last
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
    public int countTotalWars(String guildName, @NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return -1;
        }

        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `guild_name` = ? " +
                        "AND `war_log_id` >= ? AND `war_log_id` < ?",
                guildName, first, last
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
    public int countSuccessWarsSum() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `war_log_id` IS NOT NULL AND `territory_log_id` IS NOT NULL"
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
    public int countTotalWarsSum() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `war_log_id` IS NOT NULL"
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

    @Override
    public int countSuccessWarsSum(@NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return -1;
        }

        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `war_log_id` IS NOT NULL AND `territory_log_id` IS NOT NULL " +
                        "AND `war_log_id` >= ? AND `war_log_id` < ?",
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

    @Override
    public int countTotalWarsSum(@NotNull Date start, @NotNull Date end) {
        int first = getFirstWarLogIdAfter(start);
        int last = getFirstWarLogIdAfter(end);
        if (first == -1 || last == -1) {
            return -1;
        }

        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_war_log` WHERE `war_log_id` IS NOT NULL " +
                        "AND `war_log_id` >= ? AND `war_log_id` < ?",
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

    @Nullable
    public List<GuildWarLog> findAllOfWarLogIdIn(List<Integer> warLogIds) {
        if (warLogIds.isEmpty()) {
            return new ArrayList<>();
        }

        String placeHolder = String.format("(%s)",
                warLogIds.stream().map(i -> "?").collect(Collectors.joining(", "))
        );

        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_war_log` WHERE `war_log_id` IN " + placeHolder,
                warLogIds.toArray()
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
    public List<GuildWarLog> findAllOfTerritoryLogIdIn(List<Integer> territoryLogIds) {
        if (territoryLogIds.isEmpty()) {
            return new ArrayList<>();
        }

        String placeHolder = String.format("(%s)",
                territoryLogIds.stream().map(i -> "?").collect(Collectors.joining(", "))
        );

        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_war_log` WHERE `territory_log_id` IN " + placeHolder,
                territoryLogIds.toArray()
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
    public List<GuildWarLog> findAll() {
        throw new Error("Find all not implemented: records size may be too large");
    }

    @Override
    public boolean update(@NotNull GuildWarLog entity) {
        throw new Error("Update not implemented: records are not expected to be updated");
    }

    @Override
    public boolean delete(@NotNull GuildWarLogId guildWarLogId) {
        throw new Error("Delete not implemented: records are not expected to be deleted");
    }
}
