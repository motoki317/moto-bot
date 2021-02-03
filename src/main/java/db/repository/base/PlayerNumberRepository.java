package db.repository.base;

import db.model.playerNumber.PlayerNumber;
import db.model.playerNumber.PlayerNumberId;
import db.repository.Repository;

import java.util.Date;

public interface PlayerNumberRepository extends Repository<PlayerNumber, PlayerNumberId> {
    /**
     * Returns an entry with the maximum number of player number.
     * @return An entry.
     */
    PlayerNumber max();

    /**
     * Returns an entry with the minimum number of player number.
     * @return An entry.
     */
    PlayerNumber min();

    /**
     * Returns the oldest date that is stored.
     * @return Oldest date.
     */
    Date oldestDate();

    /**
     * Deletes all stored entries.
     * @return {@code true} if success.
     */
    boolean deleteAll();
}
