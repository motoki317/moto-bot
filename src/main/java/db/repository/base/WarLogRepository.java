package db.repository.base;

import db.model.warLog.WarLog;
import db.model.warLog.WarLogId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public interface WarLogRepository extends Repository<WarLog, WarLogId> {
    /**
     * Creates a new war log entity and retrieves last insert id (which corresponds to the given entity).
     * @param entity Entity to create.
     * @return Last insert id. 0 if something went wrong.
     */
    int createAndGetLastInsertId(@NotNull WarLog entity);

    /**
     * Finds all logs that are contained in the given list of IDs.
     * @param ids List of IDs.
     * @return List of logs.
     */
    @Nullable
    List<WarLog> findAllIn(List<Integer> ids);

    /**
     * Finds all records that are NOT marked as `ended`.
     * @return List of records.
     */
    @Nullable
    List<WarLog> findAllNotEnded();

    /**
     * Finds all records that are NOT marked as `log_ended`.
     * @return List of records.
     */
    @Nullable
    List<WarLog> findAllLogNotEnded();
}
