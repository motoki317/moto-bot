package api.wynn;

import api.wynn.structs.GuildList;
import log.Logger;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;

class V3Guilds {
    private static final String guildListPath = "/v3/guild/list/guild";

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    V3Guilds(String baseURL, RateLimiter rateLimiter, Logger logger) {
        this.baseURL = baseURL;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    GuildList mustGetGuildList() {
        this.rateLimiter.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(this.baseURL + guildListPath);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            return new GuildList(body);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing guild list", e);
            return null;
        }
    }
}
