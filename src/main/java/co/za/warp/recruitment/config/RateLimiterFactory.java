package co.za.warp.recruitment.config;


import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;

public class RateLimiterFactory {
    public static final int OUTBOUND_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 8;
    public static final int UPLOAD_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 300;
    private static final String AUTH_RATE_LIMITER_NAME = "OutboundAuthRateLimiter";
    private static final String UPLOAD_RATE_LIMITER_NAME = "OutboundUploadRateLimiter";

    public static RateLimiter createAuthRateLimiter(int duration, int limit, int timeout) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(8)
                .timeoutDuration(Duration.ofSeconds(0))
                .build();
        return RateLimiter.of(AUTH_RATE_LIMITER_NAME, config);
    }

    public static RateLimiter createUploadRateLimiter(int duration, int limit, int timeout) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(duration))
                .limitForPeriod(limit)
                .timeoutDuration(Duration.ofSeconds(timeout))
                .build();
        return RateLimiter.of(UPLOAD_RATE_LIMITER_NAME, config);
    }

    public static RateLimiter createAuthRateLimiter() {
        return createAuthRateLimiter(1, OUTBOUND_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS, 0);
    }

    public static RateLimiter createUploadRateLimiter() {
        return createUploadRateLimiter(UPLOAD_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS, 1, 0);
    }
}
