package db.repository.base;

import db.model.musicInterruptedGuild.MusicInterruptedGuild;
import db.model.musicInterruptedGuild.MusicInterruptedGuildId;
import db.repository.Repository;

import java.util.List;

public interface MusicInterruptedGuildRepository extends Repository<MusicInterruptedGuild, MusicInterruptedGuildId> {
    /**
     * Create all entries.
     * @param guilds Entries.
     * @return {@code true} if success.
     */
    boolean createAll(List<MusicInterruptedGuild> guilds);

    /**
     * Deletes all entries.
     * @return {@code true} if success.
     */
    boolean deleteAll();
}
