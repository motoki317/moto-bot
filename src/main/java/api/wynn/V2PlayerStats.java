package api.wynn;

import api.wynn.structs.Player;
import log.Logger;
import utils.HttpUtils;
import utils.StatusCodeException;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;
import utils.rateLimit.RateLimitException;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

class V2PlayerStats {
    private static final String playerStatisticsUrl = "https://api.wynncraft.com/v2/player/%s/stats";
    private static final DataCache<String, Player> playerStatsCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );
    private static final int PLAYER_NOT_FOUND = 400;

    private final RateLimiter rateLimiter;
    private final Logger logger;

    V2PlayerStats(RateLimiter rateLimiter, Logger logger) {
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    Player getPlayerStats(String playerName, boolean forceReload) throws RateLimitException {
        Player player;
        if ((player = playerStatsCache.get(playerName)) != null && !forceReload) {
            return player;
        }

        this.rateLimiter.checkRequest();
        return requestPlayerStatistics(playerName);
    }

    @Nullable
    Player mustGetPlayerStatistics(String playerName, boolean forceReload) {
        Player player;
        if ((player = playerStatsCache.get(playerName)) != null && !forceReload) {
            return player;
        }

        this.rateLimiter.stackUpRequest();
        return requestPlayerStatistics(playerName);
    }

    @Nullable
    private Player requestPlayerStatistics(String playerName) {
        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(String.format(playerStatisticsUrl, playerName), PLAYER_NOT_FOUND);
            long end = System.nanoTime();
            if (body == null) throw new Exception("returned body was null");
            this.logger.debug(String.format("Wynn API: Requested player stats for %s, took %s ms.", playerName, (double) (end - start) / 1_000_000d));

            Player player = new Player(body);
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
