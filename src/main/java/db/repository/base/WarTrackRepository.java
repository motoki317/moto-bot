package db.repository.base;

import db.model.warTrack.WarTrack;
import db.model.warTrack.WarTrackId;
import db.repository.Repository;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;

public interface WarTrackRepository extends Repository<WarTrack, WarTrackId> {
    /**
     * Finds all war track entries with the given war log id.
     * @param id War log id.
     * @return List of entries.
     */
    @CheckReturnValue
    @Nullable
    List<WarTrack> findAllOfWarLogId(int id);

    /**
     * Deletes all entries in the table with `log_ended` flag set to true.
     * @return {@code true} if success.
     */
    @CheckReturnValue
    boolean deleteAllOfLogEnded();
}
