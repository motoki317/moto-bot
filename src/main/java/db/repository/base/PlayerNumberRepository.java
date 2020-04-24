package db.repository.base;

import db.ConnectionPool;
import db.model.playerNumber.PlayerNumber;
import db.model.playerNumber.PlayerNumberId;
import log.Logger;

import java.util.Date;

public abstract class PlayerNumberRepository extends Repository<PlayerNumber, PlayerNumberId> {
    protected PlayerNumberRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Returns an entry with the maximum number of player number.
     * @return An entry.
     */
    public abstract PlayerNumber max();

    /**
     * Returns an entry with the minimum number of player number.
     * @return An entry.
     */
    public abstract PlayerNumber min();

    /**
     * Returns the oldest date that is stored.
     * @return Oldest date.
     */
    public abstract Date oldestDate();

    /**
     * Deletes all stored entries.
     * @return {@code true} if success.
     */
    public abstract boolean deleteAll();
}
