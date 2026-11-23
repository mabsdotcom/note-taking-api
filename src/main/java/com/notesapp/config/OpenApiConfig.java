package com.notesapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata. Every endpoint requires a Bearer JWT unless it declares
 * an empty {@code @SecurityRequirements} (the auth endpoints do).
 * UI: /swagger-ui.html, spec: /v3/api-docs
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Note-Taking API",
                version = "1.0.0",
                description = "REST API for rich text notes with categories, tags, search and filtering. "
                        + "Log in via POST /api/auth/login, then click 'Authorize' and paste the accessToken.",
                contact = @Contact(name = "Note-Taking API")),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";
}
