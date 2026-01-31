package co.za.warp.recruitment.service;

import co.za.warp.recruitment.config.RateLimiterFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterIT {
    static final Logger log = Logger.getLogger(RateLimiterIT.class.getName());

    @Test
    @DisplayName("Given the AuthRateLimiter when test duration matches 8 x 1 second, we should get 8 only")
    void outboundAuthRateLimiter_allowsOnlyConfiguredRequestsPerSecond() {
        long testDurationMillis = 2_000;
        long startTimeMillis = System.currentTimeMillis();
        AtomicInteger grantedCounter = new AtomicInteger();
        var outboundAuthRateLimiter = RateLimiterFactory.createAuthRateLimiter();
        IntStream.generate(() -> 0)
                .takeWhile(i -> System.currentTimeMillis() - startTimeMillis < testDurationMillis)
                .forEach(i -> {
                    if (outboundAuthRateLimiter.acquirePermission()) {
                        grantedCounter.incrementAndGet();
                    }
                });
        int granted = grantedCounter.get();
        assertTrue(granted <= 16, "Granted more permits than allowed 8 per second: " + granted);
        log.info("Granted " + granted + " per second");
    }

    @Test
    @DisplayName("Given the UploadRateLimiter when test duration matches 1 x 5 minutes, we should get 1 only")
    @Disabled("It takes 5 minutes to run, so only to be tested in dev, no time for setting up profiles!")
    void outboundUploadRateLimiter_allowsOnlyConfiguredRequestsPerSecond() {
        long testDurationMillis = 3_00_000;
        long startTimeMillis = System.currentTimeMillis();
        AtomicInteger grantedCounter = new AtomicInteger();
        var outboundAuthRateLimiter = RateLimiterFactory.createUploadRateLimiter();
        IntStream.generate(() -> 0)
                .takeWhile(i -> System.currentTimeMillis() - startTimeMillis < testDurationMillis)
                .forEach(i -> {
                    if (outboundAuthRateLimiter.acquirePermission()) {
                        grantedCounter.incrementAndGet();
                    }
                });
        int granted = grantedCounter.get();
        assertTrue(granted == 1, "Granted more permits than allowed per second: " + granted);
        log.info("Granted " + granted + " per 300 seconds==5 Minutes");
    }


}
