package co.za.warp.recruitment.client;

import io.github.resilience4j.ratelimiter.RateLimiter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RateLimitedLineRunner {
    /**
     * Iterates through a list of passwords, and applies fn(item) under the supplied RateLimiter.
     * Stops early when fn returns non-null.
     */
    public static Optional<String> runUntilResult(
            RateLimiter limiter,
            List<String> lines,
            Function<String, Optional<String>>  fn
    ) throws Exception {
        for (String passwordCandidate : lines) {
            Optional<String> result = fn.apply(passwordCandidate);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}

