package db.repository.base;

import db.ConnectionPool;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.guildXpLeaderboard.GuildXpLeaderboardId;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class GuildXpLeaderboardRepository extends Repository<GuildXpLeaderboard, GuildXpLeaderboardId> {
    protected GuildXpLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Creates all entries from the given list.
     * @param list List of entries.
     * @return {@code true} if success.
     */
    public abstract boolean createAll(@NotNull List<GuildXpLeaderboard> list);

    /**
     * Truncates table and deletes all data.
     * @return true if success.
     */
    public abstract boolean truncateTable();
}
