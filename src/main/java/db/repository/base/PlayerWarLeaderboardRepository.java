package db.repository.base;

import db.ConnectionPool;
import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.model.playerWarLeaderboard.PlayerWarLeaderboardId;
import log.Logger;
import utils.UUID;

import javax.annotation.Nullable;
import java.util.List;

public abstract class PlayerWarLeaderboardRepository extends Repository<PlayerWarLeaderboard, PlayerWarLeaderboardId> {
    protected PlayerWarLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Retrieves records from the table where they are ordered by number of total wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    public abstract List<PlayerWarLeaderboard> getByTotalWarDescending(int limit, int offset);

    /**
     * Retrieves records from the table where they are ordered by number of success wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    public abstract List<PlayerWarLeaderboard> getBySuccessWarDescending(int limit, int offset);

    /**
     * Retrieves records from the table where they are ordered by number of survived wars.
     * @param limit Limit number of records per request.
     * @param offset For pagination.
     * @return List of records.
     */
    @Nullable
    public abstract List<PlayerWarLeaderboard> getBySurvivedWarDescending(int limit, int offset);

    /**
     * Retrieves records of the given player UUIDs.
     * Returned results are not guaranteed to be sorted.
     * @param playerUUIDs Player UUIDs.
     * @return List of records.
     */
    @Nullable
    public abstract List<PlayerWarLeaderboard> getRecordsOf(List<UUID> playerUUIDs);
}
