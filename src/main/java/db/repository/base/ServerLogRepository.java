package db.repository.base;

import db.model.serverLog.ServerLogEntry;
import db.model.serverLog.ServerLogEntryId;
import db.repository.Repository;

import java.util.List;

public interface ServerLogRepository extends Repository<ServerLogEntry, ServerLogEntryId> {
    /**
     * Find all entries in the given guild IDs.
     * @param guildIDs Guild IDs.
     * @return List of entries.
     */
    List<ServerLogEntry> findAllIn(long... guildIDs);
}
