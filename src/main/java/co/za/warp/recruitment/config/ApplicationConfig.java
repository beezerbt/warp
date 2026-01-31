package co.za.warp.recruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ApplicationConfig {

    public static final int HTTP_CONNECTION_TIMEOUT_DURATION_SECONDS = 10;

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .connectTimeout(Duration.ofSeconds(HTTP_CONNECTION_TIMEOUT_DURATION_SECONDS))
                .build();
    }

}
