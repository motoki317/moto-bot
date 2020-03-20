package api.wynn;

import api.wynn.structs.*;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import utils.rateLimit.RateLimitException;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WynnApi {
    private static final RateLimiter rateLimiterLegacy;
    private static final RateLimiter rateLimiterV2Player;

    static {
        final int maxRequestStack = 5;

        // As of Jan 15th, 2020: rate limit for legacy (including all routes) is 1200 requests / 20 minutes.
        long waitBetweenRequests = TimeUnit.MINUTES.toMillis(20) / 1200;
        System.out.printf("Wynn API: for legacy resources setting wait between requests to %s ms\n",
                waitBetweenRequests);
        rateLimiterLegacy = new WaitableRateLimiter(
                "Wynn Legacy", waitBetweenRequests, maxRequestStack
        );

        // As of Mar 20th, 2020: V2 Player (wynncraft/player) : 500 per 30 minutes
        waitBetweenRequests = TimeUnit.MINUTES.toMillis(30) / 500;
        System.out.printf("Wynn API v2: for player resources setting wait between requests to %s ms\n",
                waitBetweenRequests);
        rateLimiterV2Player = new WaitableRateLimiter(
                "Wynn V2 Player", waitBetweenRequests, maxRequestStack
        );
    }

    // ----- Legacy Routes -----
    private final LegacyPlayers legacyPlayers;
    private final LegacyTerritories legacyTerritories;
    private final LegacyGuilds legacyGuilds;
    private final LegacyGuildStats legacyGuildStats;
    private final LegacyForumId legacyForumId;
    private final LegacyGuildLeaderboard legacyGuildLeaderboard;

    // ----- V2 Routes -----
    private final V2PlayerStats v2PlayerStats;

    public WynnApi(Logger logger, TimeZone wynnTimeZone) {
        this.legacyPlayers = new LegacyPlayers(rateLimiterLegacy, logger);
        this.legacyTerritories = new LegacyTerritories(rateLimiterLegacy, logger, wynnTimeZone);
        this.legacyGuilds = new LegacyGuilds(rateLimiterLegacy, logger);
        this.legacyGuildStats = new LegacyGuildStats(rateLimiterLegacy, logger);
        this.legacyForumId = new LegacyForumId(rateLimiterLegacy, logger);
        this.legacyGuildLeaderboard = new LegacyGuildLeaderboard(rateLimiterLegacy, logger);

        this.v2PlayerStats = new V2PlayerStats(rateLimiterV2Player, logger);

        rateLimiterLegacy.setLogger(logger);
        rateLimiterV2Player.setLogger(logger);
    }

    /**
     * GET https://api.wynncraft.com/public_api.php?action=onlinePlayers
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @return Online players struct.
     */
    @Nullable
    @CheckReturnValue
    public synchronized OnlinePlayers mustGetOnlinePlayers() {
        return this.legacyPlayers.mustGetOnlinePlayers();
    }

    /**
     * Finds the world in which player is logged.
     * <br>"must" as in it does not throw {@link RateLimitException}.
     * @param playerName Player name.
     * @return World name. null if the player was not online.
     */
    @Nullable
    @CheckReturnValue
    public String mustFindPlayer(@NotNull String playerName) {
        return this.legacyPlayers.mustFindPlayer(playerName);
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
