package db.repository.base;

import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.guildXpLeaderboard.GuildXpLeaderboardId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface GuildXpLeaderboardRepository extends Repository<GuildXpLeaderboard, GuildXpLeaderboardId> {
    /**
     * Creates all entries from the given list.
     * @param list List of entries.
     * @return {@code true} if success.
     */
    boolean createAll(@NotNull List<GuildXpLeaderboard> list);

    /**
     * Retrieves the rank of given guild in xp leaderboard.
     * @param guildName Guild name.
     * @return Rank of the guild. -1 if the guild is not in, or something went wrong.
     */
    int getXPRank(@NotNull String guildName);

    /**
     * Truncates table and deletes all data.
     * @return true if success.
     */
    boolean truncateTable();
}
