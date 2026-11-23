package com.notesapp.service;

import java.util.Locale;

import com.notesapp.dto.auth.AuthResponse;
import com.notesapp.dto.auth.LoginRequest;
import com.notesapp.dto.auth.RegisterRequest;
import com.notesapp.entity.RefreshToken;
import com.notesapp.entity.Role;
import com.notesapp.entity.User;
import com.notesapp.exception.DuplicateResourceException;
import com.notesapp.exception.InvalidTokenException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.mapper.UserMapper;
import com.notesapp.repository.UserRepository;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /** New accounts always get the USER role; admins are promoted by another admin. */
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered");
        }
        User user = new User(username, email, passwordEncoder.encode(request.password()), Role.USER);
        return issueTokens(userRepository.save(user));
    }

    /** Delegates password checking (BCrypt) to Spring Security; throws BadCredentialsException on failure. */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        return issueTokens(user);
    }

    /** Refresh token rotation: the used token is revoked and a new pair is issued. */
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken current = refreshTokenService.verify(refreshTokenValue);
        User user = current.getUser();
        refreshTokenService.revoke(current);
        if (!user.isEnabled()) {
            throw new InvalidTokenException("Account is disabled");
        }
        return issueTokens(user);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenService.revokeIfPresent(refreshTokenValue);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(AppUserPrincipal.from(user));
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000, UserMapper.toResponse(user));
    }
}
