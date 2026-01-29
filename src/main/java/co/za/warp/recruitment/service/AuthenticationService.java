package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.AuthenticationApiClient;
import co.za.warp.recruitment.client.RateLimitedLineRunner;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static co.za.warp.recruitment.config.ApplicationConfig.OUTBOUND_AUTH_RATE_LIMITER_BEAN;

@Service
@SuppressWarnings("UnstableApiUsage")
public class AuthenticationService {

    private final RateLimiter rateLimiter;
    private final AuthenticationApiClient authenticationApiClient;

    @Autowired
    public AuthenticationService(
            @Qualifier(OUTBOUND_AUTH_RATE_LIMITER_BEAN)
            RateLimiter rateLimiter,
            AuthenticationApiClient authenticationApiClient) {
        this.rateLimiter = rateLimiter;
        this.authenticationApiClient = authenticationApiClient;
    }

    public Optional<String> authenticateOnce(String authUrl, String username, String password) throws Exception {
        return authenticationApiClient.authenticateOnce(authUrl, username, password);
    }

    public Optional<String> authenticateWithRateLimiter(String authUrl, String username, List<String> generatedPasswordList) throws Exception {
        Function<String, Optional<String>> authFunction = password -> {
            try {
                return authenticateOnce(authUrl, username, password);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        };

        try {
            return RateLimitedLineRunner.runUntilResult(rateLimiter, generatedPasswordList, authFunction);
        } catch (CompletionException e) {
            throw (Exception) e.getCause();
        }
    }
}