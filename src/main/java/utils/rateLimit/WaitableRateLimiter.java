package utils.rateLimit;

import java.util.concurrent.TimeUnit;

public class WaitableRateLimiter implements RateLimiter {
    private final String apiName;
    private final long waitBetweenRequests;
    private final long maxRequestStack;

    // Indicates last request time. When requestStack goes up, this is not updated.
    private long lastRequestTime;
    private int requestStack;

    public WaitableRateLimiter(String apiName, long waitBetweenRequests, long maxRequestStack) {
        this.apiName = apiName;
        this.waitBetweenRequests = waitBetweenRequests;
        this.maxRequestStack = maxRequestStack;
    }

    @Override
    public void checkRequest() throws RateLimitException {
        long timeSinceLast = Math.abs(System.currentTimeMillis() - lastRequestTime);
        long hasToWait = waitBetweenRequests * requestStack;

        if (timeSinceLast < hasToWait) {
            long backoff = hasToWait - timeSinceLast;
            if (requestStack >= maxRequestStack) {
                throw new RateLimitException(String.format(
                        "The bot is trying to request %s too quickly!" +
                                " Please wait `%s` seconds before trying again.",
                        apiName, (double) backoff / 1000d
                ), backoff, TimeUnit.MILLISECONDS);
            } else {
                requestStack++;
            }
        } else {
            lastRequestTime = System.currentTimeMillis();
            requestStack = 1;
        }
    }

    public void stackUpRequest() {
        long timeSinceLast = Math.abs(System.currentTimeMillis() - lastRequestTime);
        long hasToWait = waitBetweenRequests * requestStack;

        if (timeSinceLast < hasToWait) {
            requestStack++;
        } else {
            lastRequestTime = System.currentTimeMillis();
            requestStack = 1;
        }
    }
}
