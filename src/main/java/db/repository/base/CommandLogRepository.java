package db.repository.base;

import db.model.commandLog.CommandLog;
import db.model.commandLog.CommandLogId;
import db.repository.Repository;

public interface CommandLogRepository extends Repository<CommandLog, CommandLogId> {
}
