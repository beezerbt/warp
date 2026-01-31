package co.za.warp.recruitment.client;

import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.util.TestUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UploadApiClientIT {

    public static final String POST_HTTP_METHOD = "POST";
    @Mock
    HttpClient httpClient;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    HttpResponse<String> httpResponse;

    @Test
    void uploadZipBase64_Once_throwsOnNullUploadPayload() {
        UploadApiClient client = new UploadApiClient(httpClient);
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> client.uploadZipBase64Once("https://example.com/upload", null)
        );
        assertEquals("Payload must not be null", ex.getMessage());
        verifyNoInteractions(httpClient, objectMapper);
    }

    @Test
    void uploadZipBase64_Once_throwsOnEmptyUploadPayload() {
        UploadApiClient client = new UploadApiClient(httpClient);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> client.uploadZipBase64Once("https://example.com/upload", "")
        );
        assertEquals("Both upload URL and payload must not be blank.", ex.getMessage());
        verifyNoInteractions(httpClient, objectMapper);
    }

    @Test
    @DisplayName("Given a ZipUpload when the upload is successful then we receive an HTTP 201 and " +
            "return it to the caller with an ok true in the body")
    void uploadZipBase64_postsJsonWithBase64OnceAndReturnsHttpResult() throws Exception {
        // Arrange
        UploadApiClient uploadApiClient = TestUtility.generateMockedUploadApiClient(
                TestUtility.HTTP_STATUS_CODE_POST_SUCCEEDED,
                TestUtility.UPLOAD_API_CLIENT_201_RESPONSE_BODY_CONTENT, httpClient, httpResponse);
        // Act
        String uploadUrl = "https://example.com/upload";
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        String base64EncodedZipFileContentsAsString = Base64.getEncoder().encodeToString(zipBytes);
        HttpResultDTO result = uploadApiClient.uploadZipBase64Once(uploadUrl, base64EncodedZipFileContentsAsString);
        // Assert: returned result
        assertNotNull(result);
        assertEquals(TestUtility.HTTP_STATUS_CODE_POST_SUCCEEDED, result.statusCode());
        assertEquals(TestUtility.UPLOAD_API_CLIENT_201_RESPONSE_BODY_CONTENT, result.value());
        // Assert: request built as expected
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest req = requestCaptor.getValue();
        assertEquals(URI.create(uploadUrl), req.uri());
        assertEquals(Duration.ofSeconds(30), req.timeout().orElseThrow());
        assertEquals(POST_HTTP_METHOD, req.method());
        assertEquals(
                UploadApiClient.CONTENT_TYPE_VALUE,
                req.headers().firstValue(UploadApiClient.CONTENT_TYPE_KEY).orElseThrow()
        );
    }

    @Test
    @DisplayName("Given a ZipUpload when we simulate the upload rate as violated then we receive an HTTP 429 and " +
            "return it to the caller")
    void mockRateViolation() throws Exception {
        // Arrange
        UploadApiClient uploadApiClient = TestUtility.generateMockedUploadApiClient(
                TestUtility.HTTP_STATUS_CODE_RATE_LIMIT_EXCEEDED,
                TestUtility.UPLOAD_API_CLIENT_429_RESPONSE_BODY_CONTENT, httpClient, httpResponse);
        // Act
        String uploadUrl = "https://example.com/upload";
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        String base64EncodedZipFileContentsAsString = Base64.getEncoder().encodeToString(zipBytes);
        HttpResultDTO httpResultDTO = uploadApiClient.uploadZipBase64Once(uploadUrl, base64EncodedZipFileContentsAsString);
        // Assert: returned result
        assertNotNull(httpResultDTO);
        assertEquals(TestUtility.HTTP_STATUS_CODE_RATE_LIMIT_EXCEEDED, httpResultDTO.statusCode());
        assertEquals(TestUtility.UPLOAD_API_CLIENT_429_RESPONSE_BODY_CONTENT, httpResultDTO.value());
    }
}
