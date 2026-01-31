package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.config.RateLimiterFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static co.za.warp.recruitment.domain.UploadPayload.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ServiceTestConfiguration.class)
public class UploadServiceTest {
    @MockBean
    HttpClient httpClient;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    HttpResponse<String> httpResponse;

    @MockBean
    UploadApiClient uploadApiClient;

    RateLimiter rateLimiter;

    @SpyBean
    UploadService uploadService;

    @Test
    void setup() {
        assertNotNull(httpClient);
        assertNotNull(objectMapper);
        assertNotNull(httpResponse);
        rateLimiter = RateLimiterFactory.createUploadRateLimiter();
        assertNotNull(rateLimiter);
        assertNotNull(uploadService);
    }

    @Test
    @DisplayName("When testing the generation of the JSON payload from the zip bytes for upload we succeed")
    public void testGenerateUploadJSONPayload() throws JsonProcessingException {
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        String base64EncodedZipFileContentsAsString = Base64.getEncoder().encodeToString(zipBytes);
        String result = uploadService.generateUploadJSONPayload(zipBytes);
        assertNotNull(result, "Generated JSON payload should not be null");
        assertFalse(result.isEmpty(), "Generated JSON payload should not be empty");
        ObjectMapper mapper = new ObjectMapper();
        // Verifying that the output is a valid JSON
        assertDoesNotThrow(() -> mapper.readTree(result));
        // Extracting values from JSON
        JsonNode jsonNode = mapper.readTree(result);
        assertEquals(base64EncodedZipFileContentsAsString, jsonNode.get("data").asText());
        assertEquals(CANDIDATE_FIRST_NAME, jsonNode.get("name").asText());
        assertEquals(CANDIDATE_SURNAME, jsonNode.get("surname").asText());
        assertEquals(CANDIDATE_EMAIL_ADDRESS, jsonNode.get("email").asText());
    }
}
