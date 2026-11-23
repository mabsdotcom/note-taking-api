package com.notesapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notesapp.config.JwtProperties;
import com.notesapp.entity.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough";

    private final AppUserPrincipal jane = new AppUserPrincipal(1L, "jane", "pw", Role.USER, true);

    @Test
    void generatedToken_isValidForSameUser() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 60_000, 120_000));
        String token = service.generateAccessToken(jane);

        assertThat(service.extractUsername(token)).isEqualTo("jane");
        assertThat(service.isTokenValid(token, jane)).isTrue();
        assertThat(service.isTokenValid(token, new AppUserPrincipal(2L, "bob", "pw", Role.USER, true))).isFalse();
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 60_000, 120_000));
        String token = service.generateAccessToken(jane);
        JwtService other = new JwtService(new JwtProperties(SECRET + "-other", 60_000, 120_000));

        assertThatThrownBy(() -> other.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService service = new JwtService(new JwtProperties(SECRET, -1000, 120_000));
        String token = service.generateAccessToken(jane);

        assertThatThrownBy(() -> service.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecret_failsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 1000, 1000)))
                .isInstanceOf(IllegalStateException.class);
    }
}
