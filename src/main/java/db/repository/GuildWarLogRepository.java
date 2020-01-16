package db.repository;

import db.ConnectionPool;
import db.model.guildWarLog.GuildWarLog;
import db.model.guildWarLog.GuildWarLogId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuildWarLogRepository extends Repository<GuildWarLog, GuildWarLogId> {
    public GuildWarLogRepository(ConnectionPool db, Logger logger) {
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

    /**
     * Counts number of war logs for guild.
     * @param guildName Guild name.
     * @return Number of logs. -1 if something went wrong.
     */
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

    /**
     * Finds war logs for a guild, with limit and offset.
     * Ordered by descending id.
     * @param guildName Guild name.
     * @param limit Select records limit.
     * @param offset Select records offset.
     * @return List of records.
     */
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

    /**
     * Counts success wars by a guild.
     * Wars are deemed as "success" if both territory_log_id and war_log_id are logged.
     * @param guildName Guild name.
     * @return Number of success wars. -1 if something went wrong.
     */
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

    /**
     * Counts total wars done by a guild.
     * @param guildName Guild name.
     * @return Number of total wars. -1 if something went wrong.
     */
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

    /**
     * Finds all logs of the war log ID list.
     * @param warLogIds List of war_log_id.
     * @return List of logs. null if something went wrong.
     */
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
