package utils.rateLimit;

public interface RateLimiter {
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
