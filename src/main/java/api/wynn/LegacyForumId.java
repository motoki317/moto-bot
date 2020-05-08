package api.wynn;

import api.wynn.structs.ForumId;
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
 * Legacy API (rate limit is shared with legacy API)
 * GET https://api.wynncraft.com/forums/getForumId/:playerName
 */
class LegacyForumId {
    private static final String forumIdPath = "/forums/getForumId/%s";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DataCache<String, ForumId> forumIdCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );

    private final String baseURL;
    private final RateLimiter rateLimiter;
    private final Logger logger;

    LegacyForumId(String baseURL, RateLimiter rateLimiter, Logger logger) {
        this.baseURL = baseURL;
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    ForumId getForumId(String playerName) throws RateLimitException {
        ForumId forumId;
        if ((forumId = forumIdCache.get(playerName)) != null) {
            return forumId;
        }

        this.rateLimiter.checkRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(
                    String.format(this.baseURL + forumIdPath, playerName)
            );
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested forum id for %s, took %s ms.", playerName, (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            forumId = mapper.readValue(body, ForumId.class);
            if (forumId == null) {
                return null;
            }
            forumIdCache.add(playerName, forumId);
            return forumId;
        } catch (Exception e) {
            this.logger.logException(String.format("an exception occurred while requesting / parsing forum id for %s",
                    playerName
            ), e);
            return null;
        }
    }
}
