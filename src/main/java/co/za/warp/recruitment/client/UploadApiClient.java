package co.za.warp.recruitment.client;


import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.domain.UploadSubmissionRequestDTO;
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
     * Uploads a Base64-encoded ZIP file to the specified URL.
     *
     * @param uploadUrl The URL to upload the ZIP file to.
     * @param zipBytes The byte array of the ZIP file to be uploaded.
     * @return An HttpResult object containing the status code and response body.
     * @throws Exception if an error occurs during the upload process.
     */
    public HttpResultDTO uploadZipBase64Once(String uploadUrl, byte[] zipBytes) throws Exception {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("zipBytes is null/empty");
        }
        // Base64 encode
        String b64 = Base64.getEncoder().encodeToString(zipBytes);
        // JSON body
        String json = mapper.writeValueAsString(new UploadSubmissionRequestDTO(b64));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return HttpResultDTO.of(resp.statusCode(), resp.body());
    }
}
