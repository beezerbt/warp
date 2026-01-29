package co.za.warp.recruitment.client;

import co.za.warp.recruitment.domain.HttpResult;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadApiClientTest {

    @Mock HttpClient httpClient;
    @Mock ObjectMapper objectMapper;
    @Mock HttpResponse<String> httpResponse;

    @Test
    void uploadZipBase64_Once_throwsOnNullBytes() {
        UploadApiClient client = new UploadApiClient(httpClient, objectMapper);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> client.uploadZipBase64Once("https://example.com/upload", null)
        );
        assertEquals("zipBytes is null/empty", ex.getMessage());
        verifyNoInteractions(httpClient, objectMapper);
    }

    @Test
    void uploadZipBase64_Once_throwsOnEmptyBytes() {
        UploadApiClient client = new UploadApiClient(httpClient, objectMapper);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> client.uploadZipBase64Once("https://example.com/upload", new byte[0])
        );
        assertEquals("zipBytes is null/empty", ex.getMessage());
        verifyNoInteractions(httpClient, objectMapper);
    }

    @Test
    void uploadZipBase64_postsJsonWithBase64OnceAndReturnsHttpResult() throws Exception {
        // Arrange
        String uploadUrl = "https://example.com/upload";
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        String expectedB64 = Base64.getEncoder().encodeToString(zipBytes);

        // We don’t need to assert the exact JSON structure here (that’s Jackson’s job),
        // but we DO want to assert that the b64 made it into the request body.
        // So we make the mapper return a JSON string containing it.
        String mapperJson = "{\"base64Zip\":\"" + expectedB64 + "\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(mapperJson);

        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"ok\":true}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        UploadApiClient client = new UploadApiClient(httpClient, objectMapper);

        // Act
        HttpResult result = client.uploadZipBase64Once(uploadUrl, zipBytes);

        // Assert: returned result
        assertNotNull(result);
        assertEquals(201, result.statusCode());
        assertEquals("{\"ok\":true}", result.value());

        // Assert: request built as expected
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest req = requestCaptor.getValue();

        assertEquals(URI.create(uploadUrl), req.uri());
        assertEquals(Duration.ofSeconds(30), req.timeout().orElseThrow());
        assertEquals("POST", req.method());

        assertEquals(
                UploadApiClient.CONTENT_TYPE_VALUE,
                req.headers().firstValue(UploadApiClient.CONTENT_TYPE_KEY).orElseThrow()
        );

        // And ensure we actually called the mapper
        verify(objectMapper).writeValueAsString(any());
    }
}
