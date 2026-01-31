package co.za.warp.recruitment.config;


import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;

public class RateLimiterFactory {
    public static final int OUTBOUND_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 8;
    // Upload endpoint: 1 request / 5 minutes = 1 / 300 permits per second
    public static final int UPLOAD_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 300;
    public static final long UPLOAD_RATE_LIMITER_WARM_UP_PERIOD = 350L;
    public static final int MAX_WARMUP_SECONDS = 4;
    private static final String AUTH_RATE_LIMITER_BEAN = "OutboundAuthRateLimiter";
    private static final String UPLOAD_RATE_LIMITER_BEAN = "OutboundUploadRateLimiter";

    public static RateLimiter createAuthRateLimiter(int duration, int limit, int timeout) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(8)
                .timeoutDuration(Duration.ofSeconds(0))
                .build();

        return RateLimiter.of(AUTH_RATE_LIMITER_BEAN, config);
    }

    public static RateLimiter createUploadRateLimiter(int duration, int limit, int timeout) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(duration))
                .limitForPeriod(limit)
                .timeoutDuration(Duration.ofSeconds(timeout))
                .build();

        return RateLimiter.of(UPLOAD_RATE_LIMITER_BEAN, config);
    }

    public static RateLimiter createAuthRateLimiter() {
        return createAuthRateLimiter(1, 8, 0);
    }

    public static RateLimiter createUploadRateLimiter() {
        return createUploadRateLimiter(5, 1, 0);
    }
}
