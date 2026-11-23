package com.notesapp.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables automatic population of createdAt/updatedAt/createdBy/updatedBy.
 * The auditor is the username of the authenticated user, or "system" for
 * unauthenticated work such as registration, seeding and scheduled jobs.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    public static final String SYSTEM_AUDITOR = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return Optional.of(SYSTEM_AUDITOR);
            }
            return Optional.of(auth.getName());
        };
    }
}
