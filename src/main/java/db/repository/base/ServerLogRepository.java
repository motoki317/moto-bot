package db.repository.base;

import db.ConnectionPool;
import db.model.serverLog.ServerLogEntry;
import db.model.serverLog.ServerLogEntryId;
import log.Logger;

public abstract class ServerLogRepository extends Repository<ServerLogEntry, ServerLogEntryId> {
    protected ServerLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }
}
