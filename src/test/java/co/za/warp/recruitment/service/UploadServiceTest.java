package co.za.warp.recruitment.service;

import co.za.warp.recruitment.client.UploadApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static co.za.warp.recruitment.config.ApplicationConfig.OUTBOUND_UPLOAD_RATE_LIMITER_BEAN;
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

    @MockBean
    @Qualifier(OUTBOUND_UPLOAD_RATE_LIMITER_BEAN)
    @SuppressWarnings("UnstableApiUsage")
    RateLimiter rateLimiter;

    @SpyBean
    UploadService uploadService;

    @Test
    void setup() {
        assertNotNull(httpClient);
        assertNotNull(objectMapper);
        assertNotNull(httpResponse);
        assertNotNull(rateLimiter);
        assertNotNull(uploadService);
    }

    @Test
    @DisplayName("When testing the generation of the JSON payload for upload we succeed")
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
        assertEquals("Kambiz", jsonNode.get("name").asText());
        assertEquals("Eghbali Tabar-Shahri", jsonNode.get("surname").asText());
        assertEquals("kambiz.shahri@gmail.com", jsonNode.get("email").asText());
    }
}
