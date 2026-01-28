package co.za.warp.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String name,
        String surname,
        String email,
        String cvPath,
        String username,
        String password,
        String authUrl
) {}
