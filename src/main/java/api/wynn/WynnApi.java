package api.wynn;

import api.wynn.exception.RateLimitException;
import api.wynn.structs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.StatusCodeException;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WynnApi {
    private static final ObjectMapper mapper = new ObjectMapper();

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

    /**
     * GET https://api.wynncraft.com/public_api.php?action=guildStats&command={guild name}
     * @param guildName Guild name.
     * @return Guild stats.
     */
    @Nullable
    public WynnGuild getGuildStats(String guildName) {
        try {
            // TODO: guild stats cache (and auto deletion when the size gets too big)
            // might want to create a custom cache system where it deletes from oldest if certain time passes or
            // the cache gets bigger
            long start = System.nanoTime();
            String body = HttpUtils.get(
                    String.format(guildStatsUrl, HttpUtils.encodeValue(guildName))
            );
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild stats for %s, took %s ms.", guildName, (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            return mapper.readValue(body, WynnGuild.class);
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

    private static final Map<String, Integer[]> rateLimits = new HashMap<>();
    private static final Map<String, Long> minWaitMillis = new HashMap<>();
    private static Map<String, Long> lastRequest = new HashMap<>();
    private static Map<String, Integer> lastRequestStack = new HashMap<>();
    private static final int maxRequestStack = 5;

    private static Map<String, Map.Entry<Long, Player>> playerNodes = new HashMap<>();

    static {
        rateLimits.put("player", new Integer[]{750, 30});

        for (Map.Entry<String, Integer[]> e : rateLimits.entrySet())
            minWaitMillis.put(e.getKey(), TimeUnit.MINUTES.toMillis(e.getValue()[1]) / e.getValue()[0]);

        System.out.println("Wynn API v2: min. wait millis : " + minWaitMillis.toString());
    }

    static {
        new Timer().scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        updateCache();
                    }
                },
                TimeUnit.MINUTES.toMillis(10),
                TimeUnit.MINUTES.toMillis(10)
        );
    }

    private static void updateCache() {
        playerNodes.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().getKey() > TimeUnit.MINUTES.toMillis(10));
    }

    /**
     * Manages rate limit. This method should always be called when requesting API.
     * @throws RateLimitException When the bot tried to request API too quickly.
     */
    private static void checkRequest(String resource, boolean canWait, Logger logger) throws RateLimitException {
        // If it is being requested too quickly
        long timeSinceLast = Math.abs(System.currentTimeMillis() - lastRequest.getOrDefault(resource, 0L));
        long hasToWait = minWaitMillis.get(resource) * lastRequestStack.getOrDefault(resource, 0);
        if (timeSinceLast < hasToWait) {
            // If the original call can wait, and if it exceeds max stack
            long backoff = hasToWait - timeSinceLast;
            if (canWait && lastRequestStack.getOrDefault(resource, 0) > maxRequestStack) {
                throw new RateLimitException("The bot is trying to request Wynncraft API too quickly!" +
                        " Please wait `" + (double) backoff / 1000d + "` seconds before trying again.", backoff, TimeUnit.MILLISECONDS);
            } else {
                lastRequestStack.put(resource, lastRequestStack.getOrDefault(resource, 0) + 1);
                logger.debug(String.format("Wynn API: Forcing too many quick requests (%s requests in series, has to wait %s more ms)",
                        lastRequestStack.get(resource), backoff));
            }

        } else {
            lastRequest.put(resource, System.currentTimeMillis());
            lastRequestStack.put(resource, 1);
        }
    }

    private static final String playerStatisticsUrl = "https://api.wynncraft.com/v2/player/%s/stats";
    private static final int PLAYER_NOT_FOUND = 400;

    @Nullable
    public Player getPlayerStatistics(String playerName, boolean canWait, boolean forceReload) {
        final String RESOURCE = "player";

        if (playerNodes.containsKey(playerName) && !forceReload) {
            return playerNodes.get(playerName).getValue();
        }

        try {
            checkRequest(RESOURCE, canWait, this.logger);

            long start = System.nanoTime();
            String body = HttpUtils.get(String.format(playerStatisticsUrl, playerName), PLAYER_NOT_FOUND);
            long end = System.nanoTime();
            if (body == null) throw new Exception("returned body was null");
            this.logger.debug(String.format("Wynn API: Requested player stats for %s, took %s ms.", playerName, (double) (end - start) / 1_000_000d));

            Player player = new Player(body);
            playerNodes.put(playerName, new AbstractMap.SimpleEntry<>(
                    System.currentTimeMillis(),
                    player
            ));
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
