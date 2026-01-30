package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.domain.UploadPayload;
import co.za.warp.recruitment.util.RateLimitedLineRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

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

    public Optional<HttpResultDTO> uploadZipOnce(String uploadURL, byte[] zipBytes) throws Exception {
        String uploadPayloadAsJSON = generateUploadJSONPayload(zipBytes);
        return Optional.ofNullable(uploadApiClient.uploadZipBase64Once(uploadURL, uploadPayloadAsJSON));
    }

    public Optional<HttpResultDTO> uploadWithRateLimiter(String authUrl, byte[] zipBytes) throws Exception {
        Function<byte[], Optional<HttpResultDTO>> uploadFunction = zipFile -> {
            try {
                return uploadZipOnce(authUrl, zipBytes);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        };

        try {
            return RateLimitedLineRunner.rateLimitedUploadOfZipFile(rateLimiter, zipBytes, uploadFunction);
        } catch (CompletionException e) {
            throw (Exception) e.getCause();
        }
    }

    public String generateUploadJSONPayload(byte[] zipBytes) throws JsonProcessingException {
        String base64EncodedZipFileContentsAsString = Base64.getEncoder().encodeToString(zipBytes);
        UploadPayload uploadPayload = new
                UploadPayload(base64EncodedZipFileContentsAsString, "Kambiz",
                "Eghbali Tabar-Shahri",
                "kambiz.shahri@gmail.com");
        var jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        return jsonMapper.writeValueAsString(uploadPayload);
    }
}
