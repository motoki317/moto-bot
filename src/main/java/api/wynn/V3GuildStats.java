package api.wynn;

import api.wynn.structs.WynnGuild;
import log.Logger;
import org.jetbrains.annotations.Nullable;
import utils.HttpUtils;
import utils.StatusCodeException;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;
import utils.rateLimit.RateLimitException;
import utils.rateLimit.RateLimiter;

import java.util.concurrent.TimeUnit;

class V3GuildStats {
    private static final String guildStatsPath = "/v3/guild/%s";
    private static final DataCache<String, WynnGuild> guildStatsCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    V3GuildStats(String baseURL, RateLimiter rateLimiter, Logger logger) {
        this.baseURL = baseURL;
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
                    String.format(this.baseURL + guildStatsPath, HttpUtils.encodeValue(guildName)),
                    404
            );
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested guild stats for %s, took %s ms.", guildName, (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            WynnGuild guild = new WynnGuild(body);
            guildStatsCache.add(guildName, guild);
            return guild;
        } catch (StatusCodeException e) {
            this.logger.debug(String.format("Wynn API: Guild %s not found", guildName));
            guildStatsCache.add(guildName, null);
            return null;
        } catch (Exception e) {
            this.logger.logException(String.format("an exception occurred while requesting / parsing guild stats for %s",
                    guildName
            ), e);
            return null;
        }
    }
}
