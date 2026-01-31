package co.za.warp.recruitment.util;

import co.za.warp.recruitment.domain.HttpResultDTO;
import io.github.resilience4j.ratelimiter.RateLimiter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Loops through a list of passwords, uses a Guava RateLimiter, which is marked
 * as UnstableApiUsage but that has been since 2011 and used in many production systems.
 * So, it's a red-herring.
 */
public class RateLimitedLineRunner {
    static final Logger log = Logger.getLogger(RateLimitedLineRunner.class.getName());

    /**
     * Runs the provided function on each element of the given list until a non-empty result is obtained,
     * while respecting the rate limiting provided by the RateLimiter.
     *
     * @param rateLimiter the rate limiter to control the rate of function invocations
     * @param lines       the list of elements to be processed by the function
     * @param fn          the function to be applied to each element of the list
     * @return an Optional containing the first non-empty result obtained from applying the function to an element,
     * or an empty Optional if no non-empty result is obtained
     */
    public static Optional<String> rateLimitedAuthentication(
            RateLimiter rateLimiter,
            List<String> lines,
            Function<String, Optional<String>> fn
    ) {
        Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
        Objects.requireNonNull(lines, "lines must not be null");
        Objects.requireNonNull(fn, "fn must not be null");

        for (String candidate : lines) {
            rateLimiter.acquirePermission();
            Optional<String> result = fn.apply(candidate);
            if (result != null && result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * Performs a rate-limited upload of a zip file using the provided RateLimiter and upload function.
     *
     * @param rateLimiter the RateLimiter to control the rate of upload operation
     * @param zipBytes the byte array representing the zip file to be uploaded
     * @param uploadFun the upload function to handle the zip file upload and return an Optional HttpResultDTO
     * @return an Optional containing the HttpResultDTO result of the upload operation
     */
    public static Optional<HttpResultDTO> rateLimitedUploadOfZipFile(
            RateLimiter rateLimiter,
            byte[] zipBytes,
            Function<byte[], Optional<HttpResultDTO>> uploadFun
    ) {
        Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
        Objects.requireNonNull(zipBytes, "zipBytes must not be null");
        Objects.requireNonNull(uploadFun, "uploadFun must not be null");

        long startTime = System.currentTimeMillis();
        log.info("Upload Rate Limiter - START - System time in milliseconds: " + startTime);
        rateLimiter.acquirePermission();
        long endTime = System.currentTimeMillis();
        log.info("Upload Rate Limiter - END   - System time in milliseconds: " + endTime);
        long differenceInMilliseconds = endTime - startTime;
        double differenceInSeconds = differenceInMilliseconds / 1000.0;
        log.info("Difference in time : " + differenceInSeconds + " seconds");
        Optional<HttpResultDTO> httpResultOptional = uploadFun.apply(zipBytes);
        if (httpResultOptional.isEmpty()) {
            throw new IllegalStateException("Zip upload function httpResult was empty result!!");
        }
        return httpResultOptional;
    }

}

