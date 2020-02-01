package db.repository.base;

import db.ConnectionPool;
import db.model.territoryLog.TerritoryLog;
import db.model.territoryLog.TerritoryLogId;
import log.Logger;

import javax.annotation.Nullable;
import java.util.List;

public abstract class TerritoryLogRepository extends Repository<TerritoryLog, TerritoryLogId> {
    protected TerritoryLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Retrieves MAX(id) of the territory_log table.
     * @return Max(id) if successful. -1 if not.
     */
    public abstract int lastInsertId();

    /**
     * Finds all territory logs with id from old id (exclusive) and new id (inclusive).
     * @param oldId Old last id (exclusive).
     * @param newId New last id (inclusive).
     * @return List of logs. null if something went wrong.
     */
    @Nullable
    public abstract List<TerritoryLog> findAllInRange(int oldId, int newId);

    /**
     * Finds all logs that is contained in the given list of IDs.
     * @param ids List of IDs.
     * @return List of logs.
     */
    @Nullable
    public abstract List<TerritoryLog> findAllIn(List<Integer> ids);
}
