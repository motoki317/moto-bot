package api.wynn;

import api.wynn.structs.GuildList;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;

class LegacyGuilds {
    private static final String guildListPath = "/public_api.php?action=guildList";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    LegacyGuilds(String baseURL, RateLimiter rateLimiter, Logger logger) {
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

            return mapper.readValue(body, GuildList.class);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing guild list", e);
            return null;
        }
    }
}
