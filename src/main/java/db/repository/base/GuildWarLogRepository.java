package db.repository.base;

import db.ConnectionPool;
import db.model.guildWarLog.GuildWarLog;
import db.model.guildWarLog.GuildWarLogId;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public abstract class GuildWarLogRepository extends Repository<GuildWarLog, GuildWarLogId> {
    protected GuildWarLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Counts number of war logs for guild.
     * @param guildName Guild name.
     * @return Number of logs. -1 if something went wrong.
     */
    public abstract int countGuildLogs(String guildName);

    /**
     * Finds war logs for a guild, with limit and offset.
     * Ordered by descending id.
     * @param guildName Guild name.
     * @param limit Select records limit.
     * @param offset Select records offset.
     * @return List of records.
     */
    @Nullable
    public abstract List<GuildWarLog> findGuildLogs(String guildName, int limit, int offset);

    /**
     * Counts success wars by a guild.
     * Wars are deemed as "success" if both territory_log_id and war_log_id are logged.
     * @param guildName Guild name.
     * @return Number of success wars. -1 if something went wrong.
     */
    public abstract int countSuccessWars(String guildName);

    /**
     * Counts total wars done by a guild.
     * @param guildName Guild name.
     * @return Number of total wars. -1 if something went wrong.
     */
    public abstract int countTotalWars(String guildName);

    /**
     * Counts success wars done by a guild in given time frame.
     * @param guildName Guild name.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return Number of success wars.
     */
    public abstract int countSuccessWars(String guildName, @NotNull Date start, @NotNull Date end);

    /**
     * Counts total wars done by a guild in given time frame.
     * @param guildName Guild name.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return Number of total wars.
     */
    public abstract int countTotalWars(String guildName, @NotNull Date start, @NotNull Date end);

    /**
     * Counts success wars done by any guild ever.
     * @return Number of success wars.
     */
    public abstract int countSuccessWarsSum();

    /**
     * Counts total wars done by any guild ever.
     * @return Number of total wars.
     */
    public abstract int countTotalWarsSum();

    /**
     * Counts success wars done in given time frame.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return Number of success wars.
     */
    public abstract int countSuccessWarsSum(@NotNull Date start, @NotNull Date end);

    /**
     * Counts total wars done in given time frame.
     * @param start Start date (inclusive)
     * @param end End date (exclusive).
     * @return Number of total wars.
     */
    public abstract int countTotalWarsSum(@NotNull Date start, @NotNull Date end);

    /**
     * Finds all logs which is associated with the given war log ids.
     * @param warLogIds List of war_log_id.
     * @return List of logs. null if something went wrong.
     */
    @Nullable
    public abstract List<GuildWarLog> findAllOfWarLogIdIn(List<Integer> warLogIds);

    /**
     * Finds all logs which is associated with the given territory log ids.
     * @param territoryLogIds  List of territory_log_id.
     * @return List of logs. null if something went wrong.
     */
    @Nullable
    public abstract List<GuildWarLog> findAllOfTerritoryLogIdIn(List<Integer> territoryLogIds);
}
