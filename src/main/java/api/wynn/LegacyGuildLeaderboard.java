package api.wynn;

import api.wynn.structs.WynnGuildLeaderboard;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;

class LegacyGuildLeaderboard {
    private static final String guildLeaderboardPath = "/public_api.php?action=statsLeaderboard&type=guild&timeframe=alltime";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    LegacyGuildLeaderboard(String baseURL, RateLimiter rateLimiter, Logger logger) {
        this.baseURL = baseURL;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    WynnGuildLeaderboard mustGetGuildLeaderboard() {
        try {
            this.rateLimiter.checkRequest();

            long start = System.nanoTime();
            String body = HttpUtils.get(this.baseURL + guildLeaderboardPath);
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
