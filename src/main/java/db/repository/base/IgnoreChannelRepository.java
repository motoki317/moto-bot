package db.repository.base;

import db.ConnectionPool;
import db.model.ignoreChannel.IgnoreChannel;
import db.model.ignoreChannel.IgnoreChannelId;
import log.Logger;

public abstract class IgnoreChannelRepository extends Repository<IgnoreChannel, IgnoreChannelId> {
    protected IgnoreChannelRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }
}
