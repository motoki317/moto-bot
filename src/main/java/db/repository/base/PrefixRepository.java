package db.repository.base;

import db.ConnectionPool;
import db.model.prefix.Prefix;
import db.model.prefix.PrefixId;
import log.Logger;

public abstract class PrefixRepository extends Repository<Prefix, PrefixId> {
    protected PrefixRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }
}
