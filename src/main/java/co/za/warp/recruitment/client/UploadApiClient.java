package co.za.warp.recruitment.client;


import co.za.warp.recruitment.domain.HttpResultDTO;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class UploadApiClient {
    public static final String CONTENT_TYPE_KEY = "Content-Type";
    public static final String CONTENT_TYPE_VALUE = "application/json";
    private final HttpClient http;

    public UploadApiClient(HttpClient http) {
        this.http = http;
    }

    /**
     * Uploads a Base64-encoded ZIP file to the specified URL.
     *
     * @param uploadUrl                 The URL to upload the ZIP file to.
     * @param completeJSONUploadPayload This a String representation of the payload, in this case JSON
     * @return An HttpResult object containing the status code and response body.
     * @throws Exception if an error occurs during the upload process.
     */
    public HttpResultDTO uploadZipBase64Once(String uploadUrl, String completeJSONUploadPayload) throws Exception {
        Preconditions.checkArgument(uploadUrl != null && completeJSONUploadPayload != null,
                "Both upload URL and payload must not be null.");
        Preconditions.checkArgument(!(uploadUrl.trim().isEmpty() || completeJSONUploadPayload.trim().isEmpty()),
                "Both upload URL and payload must not be blank.");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(completeJSONUploadPayload))
                .timeout(Duration.ofSeconds(30)) //TODO::this duration has to be verified
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return HttpResultDTO.of(resp.statusCode(), resp.body());
    }
}
