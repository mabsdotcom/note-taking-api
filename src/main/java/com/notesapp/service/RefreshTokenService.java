package com.notesapp.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import com.notesapp.config.JwtProperties;
import com.notesapp.entity.RefreshToken;
import com.notesapp.entity.User;
import com.notesapp.exception.InvalidTokenException;
import com.notesapp.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties jwtProperties) {
        this.repository = repository;
        this.jwtProperties = jwtProperties;
    }

    public RefreshToken create(User user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs());
        return repository.save(new RefreshToken(value, user, expiresAt));
    }

    /** Returns the token if it exists, is not revoked and not expired. */
    public RefreshToken verify(String token) {
        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (!refreshToken.isUsable(Instant.now())) {
            throw new InvalidTokenException("Refresh token is expired or has been revoked");
        }
        return refreshToken;
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }

    public void revokeIfPresent(String token) {
        repository.findByToken(token).ifPresent(this::revoke);
    }

    public int revokeAllForUser(Long userId) {
        return repository.revokeAllForUser(userId);
    }

    public int deleteExpiredOrRevoked() {
        return repository.deleteExpiredOrRevoked(Instant.now());
    }
}
