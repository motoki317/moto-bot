package db.repository.base;

import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.model.playerWarLeaderboard.PlayerWarLeaderboardId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;
import utils.UUID;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public interface PlayerWarLeaderboardRepository extends Repository<PlayerWarLeaderboard, PlayerWarLeaderboardId> {
    /**
     * Retrieves records from the table where they are ordered by number of total wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getByTotalWarDescending(int limit, int offset);

    /**
     * Retrieves records from the table where they are ordered by number of success wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getBySuccessWarDescending(int limit, int offset);

    /**
     * Retrieves records from the table where they are ordered by number of survived wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getBySurvivedWarDescending(int limit, int offset);

    /**
     * Retrieves records of the given player UUIDs.
     * Returned results are not guaranteed to be sorted.
     * @param playerUUIDs Player UUIDs.
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getRecordsOf(List<UUID> playerUUIDs);

    /**
     * Retrieves number of players that have done at least 1 wars during given time frame.
     * @param start Start date (inclusive).
     * @param end End date (exclusive)
     * @return Number of players. -1 if something went wrong.
     */
    int getPlayersInRange(@NotNull Date start, @NotNull Date end);

    /**
     * Retrieves records from the table where they are ordered by number of total wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getByTotalWarDescending(int limit, int offset,
                                                       @NotNull Date start, @NotNull Date end);

    /**
     * Retrieves records from the table where they are ordered by number of success wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getBySuccessWarDescending(int limit, int offset,
                                                         @NotNull Date start, @NotNull Date end);

    /**
     * Retrieves records from the table where they are ordered by number of survived wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getBySurvivedWarDescending(int limit, int offset,
                                                          @NotNull Date start, @NotNull Date end);

    /**
     * Retrieves records of the given player UUIDs.
     * Returned results are not guaranteed to be sorted.
     * @param playerUUIDs Player UUIDs.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of records.
     */
    @Nullable
    List<PlayerWarLeaderboard> getRecordsOf(List<UUID> playerUUIDs,
                                            @NotNull Date start, @NotNull Date end);

    /**
     * Retrieves guild-scoped player war leaderboard for the specified guild.
     * @param guildName Guild name.
     * @return List of records.
     */
    List<PlayerWarLeaderboard> getGuildScoped(String guildName);

    /**
     * Retrieves guild-scoped player war leaderboard for the specified guild.
     * @param guildName Guild name.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of records.
     */
    List<PlayerWarLeaderboard> getGuildScoped(String guildName,
                                              @NotNull Date start, @NotNull Date end);
}
