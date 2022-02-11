package api.wynn;

import api.wynn.structs.*;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import utils.rateLimit.RateLimitException;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class WynnApi {
    private static final String legacyBaseURL = "https://api.wynncraft.com";

    private static final RateLimiter rateLimiter;

    static {
        final int maxRequestStack = 5;

        // As of June 6th, 2020: rate limit for all endpoints (including legacy and V2) is 180 requests / 1 minute.
        long waitBetweenRequests = TimeUnit.MINUTES.toMillis(1) / 180;
        System.out.printf("Wynn API: setting wait between requests to %s ms\n", waitBetweenRequests);
        rateLimiter = new WaitableRateLimiter(
                "Wynn", waitBetweenRequests, maxRequestStack
        );
    }

    // ----- Legacy Routes -----
    private final LegacyPlayers legacyPlayers;
    private final LegacyTerritories legacyTerritories;
    private final LegacyGuilds legacyGuilds;
    private final LegacyGuildStats legacyGuildStats;
    private final LegacyForumId legacyForumId;
    private final LegacyGuildLeaderboard legacyGuildLeaderboard;
    private final LegacyItemDB legacyItemDB;

    // ----- V2 Routes -----
    private final V2PlayerStats v2PlayerStats;

    public WynnApi(Logger logger) {
        rateLimiter.setLogger(logger);

        this.legacyPlayers = new LegacyPlayers(legacyBaseURL, rateLimiter, logger);
        this.legacyTerritories = new LegacyTerritories(legacyBaseURL, rateLimiter, logger);
        this.legacyGuilds = new LegacyGuilds(legacyBaseURL, rateLimiter, logger);
        this.legacyGuildStats = new LegacyGuildStats(legacyBaseURL, rateLimiter, logger);
        this.legacyForumId = new LegacyForumId(legacyBaseURL, rateLimiter, logger);
        this.legacyGuildLeaderboard = new LegacyGuildLeaderboard(legacyBaseURL, rateLimiter, logger);
        this.legacyItemDB = new LegacyItemDB(legacyBaseURL, rateLimiter, logger);

        this.v2PlayerStats = new V2PlayerStats(rateLimiter, logger);
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=onlinePlayers
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Online-players struct.
     */
    @Nullable
    @CheckReturnValue
    public synchronized OnlinePlayers mustGetOnlinePlayers() {
        return this.legacyPlayers.mustGetOnlinePlayers();
    }

    /**
     * Finds the world in which player is logged.
     * @param playerName Player name.
     * @return World name. null if the player was not online.
     * @throws RuntimeException If the cache hasn't been made.
     */
    @Nullable
    @CheckReturnValue
    public String mustFindPlayer(@NotNull String playerName) {
        return this.legacyPlayers.mustFindPlayer(playerName);
    }

    /**
     * Finds the world in which player is logged.
     * @param playerName Player name.
     * @return World name. null if the player was not online, OR the cache hasn't been made.
     */
    @Nullable
    public String findPlayer(@NotNull String playerName) {
        try {
            return this.legacyPlayers.mustFindPlayer(playerName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=territoryList
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Territory list struct.
     */
    @Nullable
    @CheckReturnValue
    public TerritoryList mustGetTerritoryList() {
        return this.legacyTerritories.mustGetTerritoryList();
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=guildList
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Guild list.
     */
    @Nullable
    public GuildList mustGetGuildList() {
        return this.legacyGuilds.mustGetGuildList();
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=guildStats&command={guild name}
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @param guildName Guild name.
     * @return Guild stats.
     */
    @Nullable
    public WynnGuild mustGetGuildStats(String guildName) {
        return this.legacyGuildStats.mustGetGuildStats(guildName);
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=guildStats&command=:guildName
     * @param guildName Guild name.
     * @return Guild stats.
     */
    @Nullable
    public WynnGuild getGuildStats(String guildName) throws RateLimitException {
        return this.legacyGuildStats.getGuildStats(guildName);
    }

    /**
     * GET https://api.wynncraft.com/forums/getForumId/:playerName
     * @param playerName Player name
     * @return Forum id.
     */
    @Nullable
    public ForumId getForumId(String playerName) throws RateLimitException {
        return this.legacyForumId.getForumId(playerName);
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=statsLeaderboard&type=guild&timeframe=alltime
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Guild leaderboard.
     */
    @Nullable
    public WynnGuildLeaderboard mustGetGuildLeaderboard() {
        return this.legacyGuildLeaderboard.mustGetGuildLeaderboard();
    }

    /**
     * Retrieves item list.
     * @param forceReload If {@code true}, ignores internal cache and requests the API again.
     * @return Item DB.
     */
    @Nullable
    public ItemDB mustGetItemDB(boolean forceReload) {
        return this.legacyItemDB.mustGetItemDB(forceReload);
    }

    // ---- V2 Routes ----

    /**
     * Get player statistics.
     * @param playerName Player name.
     * @param forceReload Force reload.
     * @throws RateLimitException If the requests are coming in too quickly and exceeded max request stack.
     * @return Player stats.
     */
    @Nullable
    public Player getPlayerStats(String playerName, boolean forceReload) throws RateLimitException {
        return this.v2PlayerStats.getPlayerStats(playerName, forceReload);
    }

    /**
     * Get player statistics.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @param playerName Player name.
     * @param forceReload Force reload.
     * @return Player stats.
     */
    @Nullable
    public Player mustGetPlayerStatistics(String playerName, boolean forceReload) {
        return this.v2PlayerStats.mustGetPlayerStatistics(playerName, forceReload);
    }
}
