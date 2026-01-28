package co.za.warp.recruitment.client;


import co.za.warp.recruitment.domain.HttpResult;
import co.za.warp.recruitment.domain.UploadSubmissionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class UploadApiClient {
    public static final String CONTENT_TYPE_KEY = "Content-Type";
    public static final String CONTENT_TYPE_VALUE = "application/json";
    private final HttpClient http;
    private final ObjectMapper mapper;

    public UploadApiClient(HttpClient http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    /**
     * Uploads a ZIP (bytes) as Base64-encoded JSON to the provided URL.
     * Returns the HTTP status code and response body for debugging.
     */
    public HttpResult submitZipBase64(String uploadUrl, byte[] zipBytes) throws Exception {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("zipBytes is null/empty");
        }
        // Base64 encode
        String b64 = Base64.getEncoder().encodeToString(zipBytes);
        // JSON body
        String json = mapper.writeValueAsString(new UploadSubmissionRequest(b64));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return HttpResult.of(resp.statusCode(), resp.body());
    }
}
