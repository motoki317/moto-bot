package db.repository.base;

import db.ConnectionPool;
import db.model.guildLeaderboard.GuildLeaderboard;
import db.model.guildLeaderboard.GuildLeaderboardId;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public abstract class GuildLeaderboardRepository extends Repository<GuildLeaderboard, GuildLeaderboardId> {
    protected GuildLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Creates all entries from the given list.
     * @param list List of entries.
     * @return {@code true} if success.
     */
    public abstract boolean createAll(@NotNull List<GuildLeaderboard> list);

    /**
     * Retrieves all entries where the 'updated at' field is the latest.
     * @return List of entries.
     */
    @Nullable
    public abstract List<GuildLeaderboard> getLatestLeaderboard();

    /**
     * Get the newest 'updated at' field.
     * @return Newest date. null if something went wrong, or there are no entries.
     */
    @Nullable
    public abstract Date getNewestDate();

    /**
     * Get the oldest 'updated at' field.
     * @return Oldest date. null if something went wrong, or there are no entries.
     */
    @Nullable
    public abstract Date getOldestDate();

    /**
     * Checks if entries exists between given two dates.
     * @param old Old date. Exclusive.
     * @param newer New date. Exclusive.
     * @return Date. null if something went wrong, or the value doesn't exist.
     */
    @Nullable
    public abstract Date getNewestDateBetween(@NotNull Date old, @NotNull Date newer);

    /**
     * Retrieves level rank of the guild from the leaderboard.
     * @param guildName Guild name.
     * @return Level rank. -1 if something went wrong.
     */
    public abstract int getLevelRank(String guildName);

    /**
     * Deletes all entries older than the given date.
     * @param date Date. Exclusive.
     * @return true if success.
     */
    public abstract boolean deleteAllOlderThan(@NotNull Date date);
}
