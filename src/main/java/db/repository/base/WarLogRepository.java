package db.repository.base;

import db.ConnectionPool;
import db.model.warLog.WarLog;
import db.model.warLog.WarLogId;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public abstract class WarLogRepository extends Repository<WarLog, WarLogId> {
    protected WarLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Creates a new war log entity and retrieves last insert id (which corresponds to the given entity).
     * @param entity Entity to create.
     * @return Last insert id. 0 if something went wrong.
     */
    public abstract int createAndGetLastInsertId(@NotNull WarLog entity);

    /**
     * Finds all logs that is contained in the given list of IDs.
     * @param ids List of IDs.
     * @return List of logs.
     */
    @Nullable
    public abstract List<WarLog> findAllIn(List<Integer> ids);

    /**
     * Finds all records that is NOT marked as `ended`.
     * @return List of records.
     */
    @Nullable
    public abstract List<WarLog> findAllLogNotEnded();
}
