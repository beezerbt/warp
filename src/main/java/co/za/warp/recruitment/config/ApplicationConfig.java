package co.za.warp.recruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ApplicationConfig {

    public static final int HTTP_CONNECTION_TIMEOUT_DURATION_SECONDS = 10;
    public static final int RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 8;
    public static final int RATE_LIMIT_REFRESH_PERIODS = 1;
    public static final int RATE_LIMITER_TIMEOUT_DURATION_SECONDS = 2;
    public static final String RATE_LIMITER_NAME = "outboundAuth";

    @Bean
    public RateLimiter outboundAuthRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(RATE_LIMIT_REFRESH_PERIODS))
                .limitForPeriod(RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS)                    // <= 8 per second (safe under 10/s)
                .timeoutDuration(Duration.ofSeconds(RATE_LIMITER_TIMEOUT_DURATION_SECONDS)) // wait up to 2s for permission
                .build();

        return RateLimiter.of(RATE_LIMITER_NAME, config);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_CONNECTION_TIMEOUT_DURATION_SECONDS))
                .build();
    }
}
