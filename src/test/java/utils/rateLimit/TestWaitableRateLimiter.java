package utils.rateLimit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestWaitableRateLimiter {
    @Test
    void testRateLimiter() {
        RateLimiter limiter = new WaitableRateLimiter("Test", 5, 3);

        try {
            for (int i = 0; i < 3; i++) {
                limiter.checkRequest();
            }
        } catch (RateLimitException e) {
            assert false;
        }

        Assertions.assertThrows(RateLimitException.class, limiter::checkRequest);

        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assert false;
        }

        try {
            limiter.checkRequest();
        } catch (RateLimitException e) {
            assert false;
        }
    }

    @Test
    void testWaitableRequests() {
        RateLimiter limiter = new WaitableRateLimiter("Test", 5, 3);

        for (int i = 0; i < 4; i++) {
            limiter.stackUpRequest();
        }

        Assertions.assertThrows(RateLimitException.class, limiter::checkRequest);

        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assert false;
        }

        limiter.stackUpRequest();
        try {
            limiter.checkRequest();
        } catch (RateLimitException e) {
            assert false;
        }
    }
}
