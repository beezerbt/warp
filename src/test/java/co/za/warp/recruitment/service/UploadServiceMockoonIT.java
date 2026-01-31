package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.config.RateLimiterFactory;
import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.util.TestUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static co.za.warp.recruitment.util.TestUtility.HTTP_STATUS_CODE_POST_SUCCEEDED;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(classes = ScanAllConfig.class)
public class UploadServiceMockoonIT {

    @Autowired
    HttpClient httpClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UploadApiClient uploadApiClient;

    RateLimiter uploadRateLimiter;

    @Autowired
    UploadService uploadService;

    @Autowired
    ZippingService zippingService;

    @Test
    void setup() {
        assertNotNull(httpClient);
        assertNotNull(objectMapper);
        uploadRateLimiter = RateLimiterFactory.createUploadRateLimiter();
        assertNotNull(uploadRateLimiter);
        assertNotNull(uploadService);
        assertNotNull(uploadRateLimiter);
        assertNotNull(zippingService);
    }

    @Test
    @DisplayName("Given CUT when we generate a Zip and send it to a Mockoon local URL we succeed")
    void uploadZipSuccess() throws Exception {
        Path submissionZipFilePath = TestUtility.createSubmissionZip();
        assertTrue(Files.exists(submissionZipFilePath), "zip file should exist inside projectRoot");
        assertTrue(Files.size(submissionZipFilePath) > 0, "zip file should not be empty");
        byte[] zipBytes = Files.readAllBytes(submissionZipFilePath);
        assertTrue(zipBytes.length > 0, "zip file should not be empty");
        Optional<HttpResultDTO> result = uploadService.uploadZipOnce("http://localhost:3001/warp/submitcv", zipBytes);
        assertTrue(result.isPresent());
        assertEquals(HTTP_STATUS_CODE_POST_SUCCEEDED, result.get().statusCode());
    }
}
