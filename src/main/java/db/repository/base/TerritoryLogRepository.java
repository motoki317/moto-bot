package db.repository.base;

import db.model.territoryLog.TerritoryActivity;
import db.model.territoryLog.TerritoryLog;
import db.model.territoryLog.TerritoryLogId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public interface TerritoryLogRepository extends Repository<TerritoryLog, TerritoryLogId> {
    /**
     * Retrieves MAX(id) of the territory_log table.
     * @return Max(id) if successful. -1 if not.
     */
    int lastInsertId();

    /**
     * Finds all territory logs with id from old id (exclusive) and new id (inclusive).
     * @param oldId Old last id (exclusive).
     * @param newId New last id (inclusive).
     * @return List of logs. null if something went wrong.
     */
    @Nullable
    List<TerritoryLog> findAllInRange(int oldId, int newId);

    /**
     * Finds all logs that are contained in the given list of IDs.
     * @param ids List of IDs.
     * @return List of logs.
     */
    @Nullable
    List<TerritoryLog> findAllIn(List<Integer> ids);

    /**
     * Counts territory log with the given territory name.
     * @param territoryName Exact territory name.
     * @return Territory log count. -1 if something went wrong.
     */
    int territoryLogCount(String territoryName);

    /**
     * Retrieves territory log with the given territory name ordered by descending time.
     * @param territoryName Exact territory name.
     * @param limit Limit.
     * @param offset Offset.
     * @return List of territory logs.
     */
    @Nullable
    List<TerritoryLog> territoryLogs(String territoryName, int limit, int offset);

    /**
     * Retrieves how many territory logs there are for every territory.
     * @return List of territory activities.
     */
    @Nullable
    List<TerritoryActivity> territoryActivity();

    /**
     * Retrieves how many territory logs there are for every territory in the specified range.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return List of territory activities.
     */
    List<TerritoryActivity> territoryActivity(@NotNull Date start, @NotNull Date end);
}
