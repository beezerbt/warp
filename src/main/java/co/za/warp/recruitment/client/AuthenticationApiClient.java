package co.za.warp.recruitment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * This class provides methods for authenticating against a specified authentication URL
 * with basic authentication credentials. It supports both plain text URL responses and
 * JSON responses with various field name possibilities.
 */
@Component
public class AuthenticationApiClient {

    static final Logger log = Logger.getLogger(AuthenticationApiClient.class.getName());

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<String> authenticateOnce(String authUrl, String username, String password) throws Exception {
        log.info("START - Authenticating once");
        var userNamePasswordToEncode = username + ":" + password;
        log.info("userNamePasswordToEncode: " + userNamePasswordToEncode);
        String base64EncodedUserNamePasswordAsString = Base64.getEncoder().encodeToString((userNamePasswordToEncode).getBytes(StandardCharsets.UTF_8));
        log.info("Request basic auth: " + base64EncodedUserNamePasswordAsString);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Authorization", "Basic " + base64EncodedUserNamePasswordAsString)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            log.severe("http.send response status code was: " + resp.statusCode() + " and the body was:" + resp.body());
            return Optional.empty();
        } else {
            log.info("SUCCESS! http.send response status code was: " + resp.statusCode() + " and the body was:" + resp.body());
        }
        String body = resp.body() == null ? "" : resp.body().trim();
        // Some APIs return plain text URL; others return JSON. Support both.
        if (body.startsWith("http://") || body.startsWith("https://")) {
            return Optional.of(body);
        }
        JsonNode node = mapper.readTree(body);
        // best-effort guesses of field names
        for (String key : new String[]{"url", "temporaryUrl", "temporary_url", "uploadUrl", "upload_url"}) {
            if (node.hasNonNull(key) && node.get(key).isTextual()) {
                log.info("key: " + key + " value: " + node.get(key).asText());
                return Optional.of(node.get(key).asText());
            }
        }
        log.info("END - Authenticating once");
        return Optional.empty();
    }
}
