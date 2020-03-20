package utils.rateLimit;

import log.Logger;

public interface RateLimiter {
    /**
     * Sets logger for this rate limiter.
     * @param logger Logger instance.
     */
    void setLogger(Logger logger);

    /**
     * Manages rate limit. This method should always be called when requesting API.
     * @throws RateLimitException When the bot tried to request API too quickly.
     */
    void checkRequest() throws RateLimitException;

    /**
     * Manages rate limit.
     * To be called on API calls which cannot handle {@link RateLimitException}.
     */
    void stackUpRequest();
}
