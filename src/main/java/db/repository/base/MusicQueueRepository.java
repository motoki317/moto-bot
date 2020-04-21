package db.repository.base;

import db.ConnectionPool;
import db.model.musicQueue.MusicQueueEntry;
import db.model.musicQueue.MusicQueueEntryId;
import log.Logger;

import javax.annotation.Nullable;
import java.util.List;

public abstract class MusicQueueRepository extends Repository<MusicQueueEntry, MusicQueueEntryId> {
    protected MusicQueueRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Deletes all entries of the guild.
     * @param guildId Guild ID.
     * @return {@code true} if success.
     */
    public abstract boolean deleteGuildMusicQueue(long guildId);

    /**
     * Saves all given entries.
     * @param queue Music queue.
     * @return {@code true} if success.
     */
    public abstract boolean saveGuildMusicQueue(List<MusicQueueEntry> queue);

    /**
     * Retrieves guild's music queue, ordered by their index.
     * @param guildId Guild ID.
     * @return Music queue.
     */
    @Nullable
    public abstract List<MusicQueueEntry> getGuildMusicQueue(long guildId);
}
