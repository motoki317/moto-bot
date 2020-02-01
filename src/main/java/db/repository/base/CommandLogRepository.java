package db.repository.base;

import db.ConnectionPool;
import db.model.commandLog.CommandLog;
import db.model.commandLog.CommandLogId;
import log.Logger;

public abstract class CommandLogRepository extends Repository<CommandLog, CommandLogId> {
    protected CommandLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }
}
