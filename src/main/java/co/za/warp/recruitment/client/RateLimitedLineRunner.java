package co.za.warp.recruitment.client;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Loops through a list of passwords, uses a Guava RateLimiter, which is marked
 * as UnstableApiUsage but that has been since 2011 and used in many production systems.
 * So, it's a red-herring.
 */
@SuppressWarnings("UnstableApiUsage")
public class RateLimitedLineRunner {

    /**
     * Runs the provided function on each element of the given list until a non-empty result is obtained,
     * while respecting the rate limiting provided by the RateLimiter.
     *
     * @param rateLimiter the rate limiter to control the rate of function invocations
     * @param lines the list of elements to be processed by the function
     * @param fn the function to be applied to each element of the list
     * @return an Optional containing the first non-empty result obtained from applying the function to an element,
     *         or an empty Optional if no non-empty result is obtained
     */
    public static Optional<String> runUntilResult(
            RateLimiter rateLimiter,
            List<String> lines,
            Function<String, Optional<String>> fn
    ) {
        Preconditions.checkNotNull(rateLimiter, "rateLimiter must not be null");
        Preconditions.checkNotNull(lines, "lines must not be null");
        Preconditions.checkNotNull(fn, "fn must not be null");

        for (String candidate : lines) {
            rateLimiter.acquire();
            Optional<String> result = fn.apply(candidate);
            if (result != null && result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

}

