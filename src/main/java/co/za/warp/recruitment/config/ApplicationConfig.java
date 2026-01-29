package co.za.warp.recruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig {

    public static final int MAX_WARMUP_SECONDS = 4;
    public static final int HTTP_CONNECTION_TIMEOUT_DURATION_SECONDS = 10;
    public static final double OUTBOUND_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 8.0;
    // Upload endpoint: 1 request / 5 minutes = 1 / 300 permits per second
    public static final double UPLOAD_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS = 1.0/300.0;
    public static final long UPLOAD_RATE_LIMITER_WARM_UP_PERIOD = 350L;
    public static final String OUTBOUND_AUTH_RATE_LIMITER_BEAN = "outboundAuthRateLimiter";
    public static final String OUTBOUND_UPLOAD_RATE_LIMITER_BEAN = "outboundUploadRateLimiter";

    @Bean(name = OUTBOUND_AUTH_RATE_LIMITER_BEAN)
    @SuppressWarnings("UnstableApiUsage")
    public RateLimiter outboundAuthRateLimiter() {
        // Auth endpoint: 10 requests / second max.
        return RateLimiter.create(OUTBOUND_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS, MAX_WARMUP_SECONDS, TimeUnit.SECONDS);
    }

    @Bean(name = OUTBOUND_UPLOAD_RATE_LIMITER_BEAN)
    @SuppressWarnings("UnstableApiUsage")
    public RateLimiter outboundUploadRateLimiter() {
        return RateLimiter.create(UPLOAD_RATE_LIMITER_MAX_ATTEMPTS_PER_SECONDS, UPLOAD_RATE_LIMITER_WARM_UP_PERIOD, TimeUnit.SECONDS);
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
