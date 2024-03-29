package api.wynn;

import api.wynn.structs.*;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import utils.rateLimit.RateLimitException;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class WynnApi {
    private static final String baseURL = "https://api.wynncraft.com";

    private static final RateLimiter rateLimiter;

    static {
        final int maxRequestStack = 5;

        // As of June 6th, 2020: rate limit for all endpoints (including legacy and V2) is 180 requests / 1 minute.
        long waitBetweenRequests = TimeUnit.MINUTES.toMillis(1) / 180;
        org.slf4j.Logger logger = LoggerFactory.getLogger(WynnApi.class);
        logger.info(String.format("Wynn API: setting wait between requests to %s ms", waitBetweenRequests));
        rateLimiter = new WaitableRateLimiter(
                "Wynn", waitBetweenRequests, maxRequestStack
        );
    }

    // ----- Legacy Routes -----
    private final LegacyForumId legacyForumId;
    // private final LegacyItemDB legacyItemDB;

    // ----- V2 Routes -----
    private final V2PlayerStats v2PlayerStats;

    // ----- V3 Routes -----
    private final V3Players v3Players;
    private final V3Territories v3Territories;
    private final V3Guilds v3Guilds;
    private final V3GuildStats v3GuildStats;
    private final V3GuildLeaderboard v3GuildLeaderboard;

    public WynnApi(Logger logger) {
        rateLimiter.setLogger(logger);

        this.legacyForumId = new LegacyForumId(baseURL, rateLimiter, logger);
        // this.legacyItemDB = new LegacyItemDB(baseURL, rateLimiter, logger);

        this.v2PlayerStats = new V2PlayerStats(baseURL, rateLimiter, logger);

        this.v3Players = new V3Players(baseURL, rateLimiter, logger);
        this.v3Territories = new V3Territories(baseURL, rateLimiter, logger);
        this.v3Guilds = new V3Guilds(baseURL, rateLimiter, logger);
        this.v3GuildStats = new V3GuildStats(baseURL, rateLimiter, logger);
        this.v3GuildLeaderboard = new V3GuildLeaderboard(baseURL, rateLimiter, logger);
    }

    /**
     * Retrieve online players.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Online-players struct.
     */
    @Nullable
    @CheckReturnValue
    public synchronized OnlinePlayers mustGetOnlinePlayers() {
        return this.v3Players.mustGetOnlinePlayers();
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
        return this.v3Players.mustFindPlayer(playerName);
    }

    /**
     * Finds the world in which player is logged.
     * @param playerName Player name.
     * @return World name. null if the player was not online, OR the cache hasn't been made.
     */
    @Nullable
    public String findPlayer(@NotNull String playerName) {
        try {
            return this.v3Players.mustFindPlayer(playerName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieve territory list.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Territory list struct.
     */
    @Nullable
    @CheckReturnValue
    public TerritoryList mustGetTerritoryList() {
        return this.v3Territories.mustGetTerritoryList();
    }

    /**
     * Retrieve guild list.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Guild list.
     */
    @Nullable
    public GuildList mustGetGuildList() {
        return this.v3Guilds.mustGetGuildList();
    }

    /**
     * Retrieve guild stats.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @param guildName Guild name.
     * @return Guild stats.
     */
    @Nullable
    public WynnGuild mustGetGuildStats(String guildName) {
        return this.v3GuildStats.mustGetGuildStats(guildName);
    }

    /**
     * Retrieve guild stats.
     * @param guildName Guild name.
     * @return Guild stats.
     */
    @Nullable
    public WynnGuild getGuildStats(String guildName) throws RateLimitException {
        return this.v3GuildStats.getGuildStats(guildName);
    }

    /**
     * Retrieve player forum id.
     * @param playerName Player name
     * @return Forum id.
     */
    @Nullable
    public ForumId getForumId(String playerName) throws RateLimitException {
        return this.legacyForumId.getForumId(playerName);
    }

    /**
     * Retrieve guild leaderboard.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Guild leaderboard.
     */
    @Nullable
    public WynnGuildLeaderboard mustGetGuildLeaderboard() {
        return this.v3GuildLeaderboard.mustGetGuildLeaderboard();
    }

    /**
     * Retrieves item list.
     * @param forceReload If {@code true}, ignores internal cache and requests the API again.
     * @return Item DB.
     */
    @Nullable
    public ItemDB mustGetItemDB(boolean forceReload) {
        throw new RuntimeException("legacy item db api has been removed");
        // return this.legacyItemDB.mustGetItemDB(forceReload);
    }

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
