package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.config.RateLimiterFactory;
import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.domain.UploadPayload;
import co.za.warp.recruitment.util.RateLimitedLineRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static co.za.warp.recruitment.domain.UploadPayload.*;

@Service
public class UploadService {
    private final RateLimiter rateLimiter;
    private final UploadApiClient uploadApiClient;

    @Autowired
    public UploadService(
            UploadApiClient uploadApiClient) {
        this.uploadApiClient = uploadApiClient;
        this.rateLimiter = RateLimiterFactory.createUploadRateLimiter();
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
                UploadPayload(base64EncodedZipFileContentsAsString,
                CANDIDATE_FIRST_NAME,
                CANDIDATE_SURNAME,
                CANDIDATE_EMAIL_ADDRESS);
        var jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        return jsonMapper.writeValueAsString(uploadPayload);
    }

    public String generateUploadJSONPayload(Path zipFileToSubmitPath) throws IOException {
        String base64EncodedZipFileContentsAsString = Base64.getEncoder().encodeToString(Files.readAllBytes(zipFileToSubmitPath));
        UploadPayload uploadPayload = new
                UploadPayload(base64EncodedZipFileContentsAsString,
                CANDIDATE_FIRST_NAME,
                CANDIDATE_SURNAME,
                CANDIDATE_EMAIL_ADDRESS);
        var jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        return jsonMapper.writeValueAsString(uploadPayload);
    }
}
