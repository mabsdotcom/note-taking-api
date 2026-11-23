package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.notesapp.TestData;
import com.notesapp.config.JwtProperties;
import com.notesapp.entity.RefreshToken;
import com.notesapp.entity.User;
import com.notesapp.exception.InvalidTokenException;
import com.notesapp.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, new JwtProperties("x".repeat(40), 1000, 60_000));
        user = TestData.user(1L, "jane");
    }

    @Test
    void create_generatesRandomTokenWithExpiry() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken a = service.create(user);
        RefreshToken b = service.create(user);

        assertThat(a.getToken()).hasSizeGreaterThan(40).isNotEqualTo(b.getToken());
        assertThat(a.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void verify_unknownToken_throws() {
        when(repository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("nope")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verify_expiredToken_throws() {
        when(repository.findByToken("old"))
                .thenReturn(Optional.of(new RefreshToken("old", user, Instant.now().minusSeconds(1))));

        assertThatThrownBy(() -> service.verify("old")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verify_revokedToken_throws() {
        RefreshToken token = new RefreshToken("t", user, Instant.now().plusSeconds(60));
        token.setRevoked(true);
        when(repository.findByToken("t")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verify("t")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void revokeIfPresent_marksTokenRevoked() {
        RefreshToken token = new RefreshToken("t", user, Instant.now().plusSeconds(60));
        when(repository.findByToken("t")).thenReturn(Optional.of(token));

        service.revokeIfPresent("t");

        assertThat(token.isRevoked()).isTrue();
        verify(repository).save(token);
    }
}
