package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.notesapp.TestData;
import com.notesapp.dto.auth.AuthResponse;
import com.notesapp.dto.auth.LoginRequest;
import com.notesapp.dto.auth.RegisterRequest;
import com.notesapp.entity.RefreshToken;
import com.notesapp.entity.Role;
import com.notesapp.entity.User;
import com.notesapp.exception.DuplicateResourceException;
import com.notesapp.exception.InvalidTokenException;
import com.notesapp.repository.UserRepository;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private AuthService authService;

    @Test
    void register_hashesPassword_assignsUserRole_andReturnsTokens() {
        when(userRepository.existsByUsernameIgnoreCase("jane")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("$2a$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(AppUserPrincipal.class))).thenReturn("access");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenService.create(any(User.class)))
                .thenAnswer(inv -> new RefreshToken("refresh", inv.getArgument(0), Instant.now().plusSeconds(60)));

        AuthResponse response = authService.register(new RegisterRequest("jane", "Jane@Example.com", "Secret@123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("$2a$hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void register_withTakenUsername_throwsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("jane")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jane", "j@x.com", "Secret@123")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withTakenEmail_throwsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("jane")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("j@x.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jane", "j@x.com", "Secret@123")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void login_withWrongPassword_propagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane", "nope")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_success_returnsTokens() {
        User user = TestData.user(1L, "jane");
        AppUserPrincipal principal = AppUserPrincipal.from(user);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(refreshTokenService.create(user)).thenReturn(new RefreshToken("r", user, Instant.now().plusSeconds(60)));

        assertThat(authService.login(new LoginRequest("jane", "pw")).user().username()).isEqualTo("jane");
    }

    @Test
    void refresh_rotatesToken() {
        User user = TestData.user(1L, "jane");
        RefreshToken old = new RefreshToken("old", user, Instant.now().plusSeconds(60));
        when(refreshTokenService.verify("old")).thenReturn(old);
        when(jwtService.generateAccessToken(any())).thenReturn("access2");
        when(refreshTokenService.create(user)).thenReturn(new RefreshToken("new", user, Instant.now().plusSeconds(60)));

        AuthResponse response = authService.refresh("old");

        verify(refreshTokenService).revoke(old);
        assertThat(response.refreshToken()).isEqualTo("new");
    }

    @Test
    void refresh_forDisabledUser_isRejected() {
        User user = TestData.user(1L, "jane");
        user.setEnabled(false);
        when(refreshTokenService.verify("old")).thenReturn(new RefreshToken("old", user, Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> authService.refresh("old")).isInstanceOf(InvalidTokenException.class);
    }
}
