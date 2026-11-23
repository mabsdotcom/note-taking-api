package com.notesapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings bound from {@code app.jwt.*}. The secret must come from the
 * JWT_SECRET environment variable outside of development.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs) {
}
