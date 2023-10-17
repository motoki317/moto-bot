package api.wynn;

import api.wynn.structs.WynnGuildLeaderboard;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;

class V3GuildLeaderboard {
    private static final String guildLeaderboardPath = "/v3/leaderboards/guildLevel";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    V3GuildLeaderboard(String baseURL, RateLimiter rateLimiter, Logger logger) {
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

            return new WynnGuildLeaderboard(body);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing guild leaderboard", e);
            return null;
        }
    }
}
