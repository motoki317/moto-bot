package utils.rateLimit;

import java.util.concurrent.TimeUnit;

public class RateLimitException extends Exception {
    private final long backoffMillis;

    RateLimitException(String message, long backoff, TimeUnit unit) {
        super(message);
        this.backoffMillis = unit.toMillis(backoff);
    }

    public long getBackoffMillis() {
        return backoffMillis;
    }
}
