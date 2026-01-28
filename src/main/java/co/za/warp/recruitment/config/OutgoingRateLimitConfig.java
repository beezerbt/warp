package co.za.warp.recruitment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OutgoingRateLimitConfig {

    @Bean
    public RateLimiter outboundAuthRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(8)                    // <= 8 per second (safe under 10/s)
                .timeoutDuration(Duration.ofSeconds(2)) // wait up to 2s for permission
                .build();

        return RateLimiter.of("outboundAuth", config);
    }
}
