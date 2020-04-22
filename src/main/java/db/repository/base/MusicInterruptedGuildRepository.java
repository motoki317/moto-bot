package db.repository.base;

import db.ConnectionPool;
import db.model.musicInterruptedGuild.MusicInterruptedGuild;
import db.model.musicInterruptedGuild.MusicInterruptedGuildId;
import log.Logger;

import java.util.List;

public abstract class MusicInterruptedGuildRepository extends Repository<MusicInterruptedGuild, MusicInterruptedGuildId> {
    protected MusicInterruptedGuildRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Create all entries.
     * @param guilds Entries.
     * @return {@code true} if success.
     */
    public abstract boolean createAll(List<MusicInterruptedGuild> guilds);

    /**
     * Deletes all entries.
     * @return {@code true} if success.
     */
    public abstract boolean deleteAll();
}
