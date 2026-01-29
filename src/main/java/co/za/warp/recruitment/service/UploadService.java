package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.AuthenticationApiClient;
import co.za.warp.recruitment.client.RateLimitedLineRunner;
import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.domain.HttpResult;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static co.za.warp.recruitment.config.ApplicationConfig.OUTBOUND_AUTH_RATE_LIMITER_BEAN;
import static co.za.warp.recruitment.config.ApplicationConfig.OUTBOUND_UPLOAD_RATE_LIMITER_BEAN;

@Service
@SuppressWarnings("UnstableApiUsage")
public class UploadService {
    private final RateLimiter rateLimiter;
    private final UploadApiClient uploadApiClient;

    @Autowired
    public UploadService(
            @Qualifier(OUTBOUND_UPLOAD_RATE_LIMITER_BEAN)
            RateLimiter rateLimiter,
            UploadApiClient uploadApiClient) {
        this.rateLimiter = rateLimiter;
        this.uploadApiClient = uploadApiClient;
    }
    public Optional<HttpResult> uploadZipOnce(String uploadURL, byte[] zipBytes) throws Exception {
        return Optional.ofNullable(uploadApiClient.uploadZipBase64Once(uploadURL, zipBytes));
    }
    public Optional<HttpResult> uploadWithRateLimiter(String authUrl, byte[] zipBytes) throws Exception {
        Function< byte[], Optional<HttpResult>> uploadFunction = zipFile -> {
            try {
                return uploadZipOnce(authUrl, zipBytes);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        };

        try {
            return RateLimitedLineRunner.uploadUntilResult(rateLimiter, zipBytes, uploadFunction);
        } catch (CompletionException e) {
            throw (Exception) e.getCause();
        }
    }
}
