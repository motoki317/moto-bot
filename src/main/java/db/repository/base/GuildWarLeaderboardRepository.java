package db.repository.base;

import db.model.guildWarLeaderboard.GuildWarLeaderboard;
import db.model.guildWarLeaderboard.GuildWarLeaderboardId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public interface GuildWarLeaderboardRepository extends Repository<GuildWarLeaderboard, GuildWarLeaderboardId> {
    /**
     * Retrieves records from the table where they are ordered by number of total wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    List<GuildWarLeaderboard> getByTotalWarDescending(int limit, int offset);

    /**
     * Retrieves records from the table where they are ordered by number of success wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    List<GuildWarLeaderboard> getBySuccessWarDescending(int limit, int offset);

    /**
     * Retrieves number of guilds that have done at least 1 wars during given time frame.
     * @param start Start date (inclusive).
     * @param end End date (exclusive)
     * @return Number of guilds. -1 if something went wrong.
     */
    int getGuildsInRange(@NotNull Date start, @NotNull Date end);

    /**
     * Retrieves records from the table where they are ordered by number of total wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of records.
     */
    @Nullable
    List<GuildWarLeaderboard> getByTotalWarDescending(int limit, int offset,
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
    List<GuildWarLeaderboard> getBySuccessWarDescending(int limit, int offset,
                                                        @NotNull Date start, @NotNull Date end);
}
