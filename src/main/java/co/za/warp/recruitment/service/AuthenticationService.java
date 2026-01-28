package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.AuthenticationApiClient;
import co.za.warp.recruitment.client.RateLimitedLineRunner;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

@Service
public class AuthenticationService {

    private final RateLimiter rateLimiter;
    private final AuthenticationApiClient authenticationApiClient;

    @Autowired
    public AuthenticationService(RateLimiter rateLimiter, AuthenticationApiClient authenticationApiClient) {
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