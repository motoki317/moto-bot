package db.repository.base;

import db.ConnectionPool;
import db.model.warTrack.WarTrack;
import db.model.warTrack.WarTrackId;
import log.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;

public abstract class WarTrackRepository extends Repository<WarTrack, WarTrackId> {
    protected WarTrackRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Finds all war track entries with the given war log id.
     * @param id War log id.
     * @return List of entries.
     */
    @CheckReturnValue
    @Nullable
    public abstract List<WarTrack> findAllOfWarLogId(int id);

    /**
     * Deletes all entries in the table with `log_ended` flag set to true.
     * @return {@code true} if success.
     */
    @CheckReturnValue
    public abstract boolean deleteAllOfLogEnded();
}
