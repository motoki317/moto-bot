package db.repository.base;

import db.ConnectionPool;
import db.model.guildWarLeaderboard.GuildWarLeaderboard;
import db.model.guildWarLeaderboard.GuildWarLeaderboardId;
import log.Logger;

import javax.annotation.Nullable;
import java.util.List;

public abstract class GuildWarLeaderboardRepository extends Repository<GuildWarLeaderboard, GuildWarLeaderboardId> {
    protected GuildWarLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Retrieves records from the table where they are ordered by number of total wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    public abstract List<GuildWarLeaderboard> getByTotalWarDescending(int limit, int offset);

    /**
     * Retrieves records from the table where they are ordered by number of success wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    public abstract List<GuildWarLeaderboard> getBySuccessWarDescending(int limit, int offset);
}
