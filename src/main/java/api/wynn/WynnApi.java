package api.wynn;

import api.wynn.structs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.StatusCodeException;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WynnApi {
    private static final ObjectMapper mapper = new ObjectMapper();

    // ----- Legacy (v1) Rate Limiter -----

    // As of Jan 15th, 2020, rate limit for legacy (all routes) is 1200 requests / 20 minutes.
    private static final int requestsPer20Minutes = 1200;
    private static final RateLimiter rateLimiterLegacy;
    private static final int maxRequestStack = 5;

    static {
        long waitBetweenRequests = TimeUnit.MINUTES.toMillis(20) / requestsPer20Minutes;
        rateLimiterLegacy = new WaitableRateLimiter(
                "Wynn Legacy", waitBetweenRequests, maxRequestStack
        );
    }


    private final Logger logger;
    private final TimeZone wynnTimeZone;

    public WynnApi(Logger logger, TimeZone wynnTimeZone) {
        this.logger = logger;
        this.wynnTimeZone = wynnTimeZone;
    }

    private static final String onlinePlayersUrl = "https://api.wynncraft.com/public_api.php?action=onlinePlayers";

    /**
     * GET https://api.wynncraft.com/public_api.php?action=onlinePlayers
     * @return Online players struct.
     */
    @Nullable
    public OnlinePlayers getOnlinePlayers() {
        rateLimiterLegacy.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(onlinePlayersUrl);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested online players list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");
            return new OnlinePlayers(body);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing online players", e);
            return null;
        }
    }

    private static final String territoryListUrl = "https://api.wynncraft.com/public_api.php?action=territoryList";

    /**
     * GET https://api.wynncraft.com/public_api.php?action=territoryList
     * @return Territory list struct.
     */
    @Nullable
    public TerritoryList getTerritoryList() {
        rateLimiterLegacy.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(territoryListUrl);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested territory list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");
            return new TerritoryList(body, this.wynnTimeZone);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing territory list", e);
            return null;
        }
    }

    private static final String guildListUrl = "https://api.wynncraft.com/public_api.php?action=guildList";

    /**
     * GET https://api.wynncraft.com/public_api.php?action=guildList
     * @return Guild list.
     */
    @Nullable
    public GuildList getGuildList() {
        rateLimiterLegacy.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(guildListUrl);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            return mapper.readValue(body, GuildList.class);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing guild list", e);
            return null;
        }
    }

    private static final String guildStatsUrl = "https://api.wynncraft.com/public_api.php?action=guildStats&command=%s";
    private static final DataCache<String, WynnGuild> guildStatsCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );

    /**
     * GET https://api.wynncraft.com/public_api.php?action=guildStats&command={guild name}
     * @param guildName Guild name.
     * @return Guild stats.
     */
    @Nullable
    public WynnGuild getGuildStats(String guildName) {
        WynnGuild guild;
        if ((guild = guildStatsCache.get(guildName)) != null) {
            return guild;
        }

        rateLimiterLegacy.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(
                    String.format(guildStatsUrl, HttpUtils.encodeValue(guildName))
            );
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild stats for %s, took %s ms.", guildName, (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            guild = mapper.readValue(body, WynnGuild.class);
            if (guild == null) {
                return null;
            }
            guildStatsCache.add(guildName, guild);
            return guild;
        } catch (Exception e) {
            this.logger.logException(String.format("an exception occurred while requesting / parsing guild stats for %s",
                    guildName
            ), e);
            return null;
        }
    }

    // ---- V2 Routes ----

    // Rate limit is dependant on each resource.
    // Known limits:
    // Player (wynncraft/player) : 750 per 30 minutes

    private static final Map<String, RateLimiter> rateLimitersV2 = new HashMap<>();

    static {
        Map<String, Integer[]> rateLimits = new HashMap<>();
        rateLimits.put("player", new Integer[]{750, 30});

        for (Map.Entry<String, Integer[]> e : rateLimits.entrySet()) {
            long waitBetweenRequests = TimeUnit.MINUTES.toMillis(e.getValue()[1]) / e.getValue()[0];
            rateLimitersV2.put(e.getKey(), new WaitableRateLimiter(
                    e.getKey() + " API", waitBetweenRequests, maxRequestStack
            ));

            System.out.println(
                    String.format("Wynn API v2: for resource %s setting wait between requests to %s ms",
                            e.getKey(), waitBetweenRequests
                    )
            );
        }
    }

    private static final String playerStatisticsUrl = "https://api.wynncraft.com/v2/player/%s/stats";
    private static final DataCache<String, Player> playerStatsCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );
    private static final int PLAYER_NOT_FOUND = 400;

    @Nullable
    public Player getPlayerStatistics(String playerName, boolean canWait, boolean forceReload) {
        final String RESOURCE = "player";

        Player player;
        if ((player = playerStatsCache.get(playerName)) != null && !forceReload) {
            return player;
        }

        try {
            RateLimiter rateLimiter = rateLimitersV2.get(RESOURCE);
            if (rateLimiter != null) {
                if (canWait) {
                    rateLimiter.checkRequest();
                } else {
                    rateLimiter.stackUpRequest();
                }
            } else {
                return null;
            }

            long start = System.nanoTime();
            String body = HttpUtils.get(String.format(playerStatisticsUrl, playerName), PLAYER_NOT_FOUND);
            long end = System.nanoTime();
            if (body == null) throw new Exception("returned body was null");
            this.logger.debug(String.format("Wynn API: Requested player stats for %s, took %s ms.", playerName, (double) (end - start) / 1_000_000d));

            player = new Player(body);
            playerStatsCache.add(playerName, player);
            return player;
        } catch (StatusCodeException e) {
            if (e.getCode() == PLAYER_NOT_FOUND) {
                // Player not found, do not log this exception to discord
                this.logger.debug(String.format("Wynn API: Player stats for %s returned 400 (expected not found)", playerName));
                return null;
            }
            this.logger.logException("an exception occurred while requesting / parsing player statistics for " + playerName, e);
            return null;
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing player statistics for " + playerName, e);
            return null;
        }
    }
}
