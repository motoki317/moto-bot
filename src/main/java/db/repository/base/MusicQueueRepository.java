package db.repository.base;

import db.model.musicQueue.MusicQueueEntry;
import db.model.musicQueue.MusicQueueEntryId;
import db.repository.Repository;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public interface MusicQueueRepository extends Repository<MusicQueueEntry, MusicQueueEntryId> {
    /**
     * Deletes all entries of the guild.
     * @param guildId Guild ID.
     * @return {@code true} if success.
     */
    boolean deleteGuildMusicQueue(long guildId);

    /**
     * Saves all given entries.
     * @param queue Music queue.
     * @return {@code true} if success.
     */
    boolean saveGuildMusicQueue(List<MusicQueueEntry> queue);

    /**
     * Retrieves guild's music queue, ordered by their index.
     * @param guildId Guild ID.
     * @return Music queue.
     */
    @Nullable
    List<MusicQueueEntry> getGuildMusicQueue(long guildId);

    /**
     * Deletes all entries older than the specified date.
     * @param threshold Deletes if older than this date.
     * @return {@code true} if success.
     */
    boolean deleteAllOlderThan(Date threshold);
}
