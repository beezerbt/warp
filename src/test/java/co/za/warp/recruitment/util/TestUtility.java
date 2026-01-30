package co.za.warp.recruitment.util;

import co.za.warp.recruitment.client.UploadApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtility {
    public static UploadApiClient generateMockedUploadApiClient(int httpStatusCode,
                                                          String responseContent,
                                                          HttpClient mockedHttpClient,
                                                          ObjectMapper mockedObjectMapper,
                                                          HttpResponse<String> mockedHttpResponse
    ) throws IOException, InterruptedException {
        byte[] zipBytes = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        String expectedB64 = Base64.getEncoder().encodeToString(zipBytes);
        String uploadJSONPayload = "{\"base64Zip\":\"" + expectedB64 + "\"}";
        when(mockedObjectMapper.writeValueAsString(any())).thenReturn(uploadJSONPayload);
        when(mockedHttpResponse.statusCode()).thenReturn(httpStatusCode);
        when(mockedHttpResponse.body()).thenReturn(responseContent);
        when(mockedHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockedHttpResponse);
        return new UploadApiClient(mockedHttpClient, mockedObjectMapper);
    }
}
