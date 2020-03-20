package utils.rateLimit;

import log.ConsoleLogger;
import log.Logger;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WaitableRateLimiter implements RateLimiter {
    private final String apiName;
    private final long waitBetweenRequests;
    private final long maxRequestStack;
    private final Object lock;

    private Logger logger;

    // Indicates last request time. When requestStack goes up, this is not updated.
    private long lastRequestTime;
    private int requestStack;

    public WaitableRateLimiter(String apiName, long waitBetweenRequests, long maxRequestStack) {
        this.apiName = apiName;
        this.waitBetweenRequests = waitBetweenRequests;
        this.maxRequestStack = maxRequestStack;
        this.lock = new Object();
        this.logger = new ConsoleLogger(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void checkRequest() throws RateLimitException {
        synchronized (this.lock) {
            long timeSinceLast = Math.abs(System.currentTimeMillis() - lastRequestTime);
            long hasToWait = waitBetweenRequests * requestStack;

            if (timeSinceLast < hasToWait) {
                long backoff = hasToWait - timeSinceLast;
                if (requestStack >= maxRequestStack) {
                    throw new RateLimitException(String.format(
                            "The bot is trying to request %s API too quickly!" +
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
    }

    public void stackUpRequest() {
        synchronized (this.lock) {
            long timeSinceLast = Math.abs(System.currentTimeMillis() - lastRequestTime);
            long hasToWait = waitBetweenRequests * requestStack;

            if (timeSinceLast < hasToWait) {
                requestStack++;

                // check rate limit and wait inside whenever absolutely needed
                if (requestStack >= 2 * maxRequestStack) {
                    long backoff = hasToWait - timeSinceLast;
                    this.logger.log(0, String.format(
                            "Rate limiter: waiting %s ms because it has reached twice the max request stack (%s).\n",
                            backoff, 2 * maxRequestStack));
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                lastRequestTime = System.currentTimeMillis();
                requestStack = 1;
            }
        }
    }
}
