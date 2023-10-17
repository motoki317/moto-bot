package api.wynn;

import api.wynn.structs.OnlinePlayers;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;

class V3Players {
    private static final String onlinePlayersPath = "/v3/player";

    // Caches for the find player method
    private static final Object onlinePlayersCacheLock = new Object();
    @Nullable
    private static OnlinePlayers onlinePlayersCache;

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    V3Players(String baseURL, RateLimiter rateLimiter, Logger logger) {
        this.baseURL = baseURL;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    synchronized OnlinePlayers mustGetOnlinePlayers() {
        this.rateLimiter.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(this.baseURL + onlinePlayersPath);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested online players list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            OnlinePlayers onlinePlayers = new OnlinePlayers(body);

            // create cache of the response
            synchronized (onlinePlayersCacheLock) {
                onlinePlayersCache = onlinePlayers;
            }

            return onlinePlayers;
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing online players", e);
            return null;
        }
    }

    /**
     * Finds the world in which player is logged.
     * @param playerName Player name.
     * @return World name. null if the player was not online.
     */
    @Nullable
    String mustFindPlayer(@NotNull String playerName) {
        if (onlinePlayersCache == null) {
            throw new RuntimeException("The cache is null");
        }

        synchronized (onlinePlayersCacheLock) {
            return onlinePlayersCache.getPlayers().getOrDefault(playerName, null);
        }
    }
}
