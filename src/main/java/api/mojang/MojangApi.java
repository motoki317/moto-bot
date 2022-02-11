package api.mojang;

import api.mojang.structs.NameHistory;
import api.mojang.structs.NullableUUID;
import log.Logger;
import org.slf4j.LoggerFactory;
import utils.UUID;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MojangApi {
    // ----- Rate limiter -----

    private static final RateLimiter rateLimiter;

    static {
        // As of Dec 29th, 2019, the rate limit is 600 reqs / 10 minutes
        // So 1 req / s at max
        final int rateLimitPerTenMinutes = 600;
        final int maxRequestStacks = 5;
        final long waitBetweenRequests = TimeUnit.MINUTES.toMillis(10) / rateLimitPerTenMinutes;
        org.slf4j.Logger logger = LoggerFactory.getLogger(MojangApi.class);
        logger.info("Setting Mojang API minimum request wait time to " + waitBetweenRequests + " ms. " +
                "(i.e. " + rateLimitPerTenMinutes + " requests per 10 minutes)");

        rateLimiter = new WaitableRateLimiter("Mojang", waitBetweenRequests, maxRequestStacks);
    }

    // ----- API instance -----

    private final CurrentUUIDs currentUUIDs;
    private final UsernameToUUID usernameToUUID;

    public MojangApi(Logger logger) {
        rateLimiter.setLogger(logger);

        this.currentUUIDs = new CurrentUUIDs(rateLimiter, logger);
        this.usernameToUUID = new UsernameToUUID(rateLimiter, logger);
    }

    /**
     * Iteratively calls getUUIDs method if size of given names is larger than 10,
     * but preferably not too large (not larger than 100).
     * @param names List of names.
     * @return Map of player names to UUIDs. null if something went wrong.
     */
    @Nullable
    public Map<String, NullableUUID> getUUIDsIterative(List<String> names) {
        return this.currentUUIDs.getUUIDsIterative(names);
    }

    /**
     * Retrieves UUID of username at given unix milliseconds time.
     * @param username Username.
     * @param unixMillis UNIX milliseconds.
     * @return UUID.
     */
    @Nullable
    public UUID mustGetUUIDAtTime(String username, long unixMillis) {
        return this.usernameToUUID.mustGetUUIDAtTime(username, unixMillis);
    }

    /**
     * Retrieves name history of given uuid.
     * @param uuid Player UUID.
     * @return Name history.
     */
    @Nullable
    public NameHistory mustGetNameHistory(UUID uuid) {
        return this.usernameToUUID.mustGetNameHistory(uuid);
    }
}
