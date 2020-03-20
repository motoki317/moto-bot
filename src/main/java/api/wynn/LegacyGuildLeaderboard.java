package api.wynn;

import api.wynn.structs.WynnGuildLeaderboard;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;

/**
 * Legacy API
 * GET https://api.wynncraft.com/public_api.php?action=statsLeaderboard&type=guild&timeframe=alltime
 */
class LegacyGuildLeaderboard {
    private static final String guildLeaderboardUrl = "https://api.wynncraft.com/public_api.php?action=statsLeaderboard&type=guild&timeframe=alltime";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final RateLimiter rateLimiter;
    private final Logger logger;

    LegacyGuildLeaderboard(RateLimiter rateLimiter, Logger logger) {
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    WynnGuildLeaderboard mustGetGuildLeaderboard() {
        try {
            this.rateLimiter.checkRequest();

            long start = System.nanoTime();
            String body = HttpUtils.get(guildLeaderboardUrl);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild leaderboard, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            return mapper.readValue(body, WynnGuildLeaderboard.class);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing guild leaderboard", e);
            return null;
        }
    }
}
