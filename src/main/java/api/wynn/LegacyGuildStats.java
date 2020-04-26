package api.wynn;

import api.wynn.structs.WynnGuild;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import org.jetbrains.annotations.Nullable;
import utils.HttpUtils;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;
import utils.rateLimit.RateLimitException;
import utils.rateLimit.RateLimiter;

import java.util.concurrent.TimeUnit;

/**
 * Legacy API
 * GET https://api.wynncraft.com/public_api.php?action=guildStats&command=:guildName
 */
class LegacyGuildStats {
    private static final String guildStatsUrl = "https://api.wynncraft.com/public_api.php?action=guildStats&command=%s";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DataCache<String, WynnGuild> guildStatsCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );

    private final RateLimiter rateLimiter;
    private final Logger logger;

    LegacyGuildStats(RateLimiter rateLimiter, Logger logger) {
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    WynnGuild mustGetGuildStats(String guildName) {
        WynnGuild guild;
        if ((guild = guildStatsCache.get(guildName)) != null) {
            return guild;
        }

        this.rateLimiter.stackUpRequest();
        return requestGuildStats(guildName);
    }

    @Nullable
    WynnGuild getGuildStats(String guildName) throws RateLimitException {
        if (guildStatsCache.exists(guildName)) {
            return guildStatsCache.get(guildName);
        }

        this.rateLimiter.checkRequest();
        return requestGuildStats(guildName);
    }

    @Nullable
    private WynnGuild requestGuildStats(String guildName) {
        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(
                    String.format(guildStatsUrl, HttpUtils.encodeValue(guildName))
            );
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild stats for %s, took %s ms.", guildName, (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            // Legacy Wynn API could return error with 200 codes
            // check for "error" field
            JsonNode node = mapper.readTree(body);
            if (node.has("error")) {
                this.logger.debug(String.format("Wynn API: Guild %s not found: %s", guildName, node.get("error").asText()));
                guildStatsCache.add(guildName, null);
                return null;
            }

            WynnGuild guild = mapper.readValue(body, WynnGuild.class);
            guildStatsCache.add(guildName, guild);
            return guild;
        } catch (Exception e) {
            this.logger.logException(String.format("an exception occurred while requesting / parsing guild stats for %s",
                    guildName
            ), e);
            return null;
        }
    }
}
