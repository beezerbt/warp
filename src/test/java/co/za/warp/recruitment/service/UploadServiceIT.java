package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.config.RateLimiterFactory;
import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.util.TestUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static co.za.warp.recruitment.util.TestUtility.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ServiceTestConfiguration.class)
class UploadServiceIT {
    @MockBean
    HttpClient httpClient;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    HttpResponse<String> httpResponse;

    @MockBean
    UploadApiClient uploadApiClient;

    RateLimiter uploadRateLimiter;

    @SpyBean
    UploadService uploadService;

    @Test
    void setup() {
        assertNotNull(httpClient);
        assertNotNull(objectMapper);
        assertNotNull(httpResponse);
        uploadRateLimiter = RateLimiterFactory.createUploadRateLimiter();
        assertNotNull(uploadRateLimiter);
        assertNotNull(uploadService);
        assertNotNull(uploadRateLimiter);
    }

    @Test
    @DisplayName("Given CUT when we simulate the server saying the rate limit has NOT been reached, then the upload succeeds")
    void uploadZipHappyPathRateLimitNotReachedAndUploadSucceeds() throws Exception {
        UploadApiClient successMockedUploadApiClient = TestUtility.generateMockedUploadApiClient(HTTP_STATUS_CODE_POST_SUCCEEDED,
                UPLOAD_API_CLIENT_201_RESPONSE_BODY_CONTENT,
                httpClient, httpResponse);
        ReflectionTestUtils.setField(uploadService, "uploadApiClient", successMockedUploadApiClient);
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        Optional<HttpResultDTO> result = uploadService.uploadZipOnce("https://example.com/upload", zipBytes);
        assertTrue(result.isPresent());
        assertEquals(HTTP_STATUS_CODE_POST_SUCCEEDED, result.get().statusCode());
        assertEquals(UPLOAD_API_CLIENT_201_RESPONSE_BODY_CONTENT, result.get().value());
    }

    @Test
    @DisplayName("Given CUT when we simulate the server saying the rate limit has been breached, then the upload fails")
    void uploadZipUnhappyPathRateLimitReachedSimulation() throws Exception {
        UploadApiClient successMockedUploadApiClient = TestUtility.generateMockedUploadApiClient(HTTP_STATUS_CODE_RATE_LIMIT_EXCEEDED,
                UPLOAD_API_CLIENT_429_RESPONSE_BODY_CONTENT,
                httpClient, httpResponse);
        ReflectionTestUtils.setField(uploadService, "uploadApiClient", successMockedUploadApiClient);
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        Optional<HttpResultDTO> result = uploadService.uploadZipOnce("https://example.com/upload", zipBytes);
        assertTrue(result.isPresent());
        assertEquals(HTTP_STATUS_CODE_RATE_LIMIT_EXCEEDED, result.get().statusCode());
        assertEquals(UPLOAD_API_CLIENT_429_RESPONSE_BODY_CONTENT, result.get().value());
    }
}