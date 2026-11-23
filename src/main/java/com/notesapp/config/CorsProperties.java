package com.notesapp.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Allowed frontend origins, bound from {@code app.cors.allowed-origins} (comma separated). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
