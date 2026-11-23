package com.notesapp.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import com.notesapp.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Issues and validates short-lived HS256-signed access tokens. */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a 256-bit key

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = properties.accessTokenExpirationMs();
    }

    public String generateAccessToken(AppUserPrincipal user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and verifies the token signature and expiry.
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
