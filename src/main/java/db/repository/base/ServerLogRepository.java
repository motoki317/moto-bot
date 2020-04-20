package db.repository.base;

import db.ConnectionPool;
import db.model.serverLog.ServerLogEntry;
import db.model.serverLog.ServerLogEntryId;
import log.Logger;

import java.util.List;

public abstract class ServerLogRepository extends Repository<ServerLogEntry, ServerLogEntryId> {
    protected ServerLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Find all entries in the given guild IDs.
     * @param guildIDs Guild IDs.
     * @return List of entries.
     */
    public abstract List<ServerLogEntry> findAllIn(long... guildIDs);
}
