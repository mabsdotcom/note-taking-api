package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.notesapp.TestData;
import com.notesapp.entity.Role;
import com.notesapp.entity.User;
import com.notesapp.exception.BadRequestException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private UserService userService;

    @Test
    void updateRole_promotesUser() {
        User user = TestData.user(2L, "bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThat(userService.updateRole(1L, 2L, Role.ADMIN).role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_ofSelf_isRejected() {
        assertThatThrownBy(() -> userService.updateRole(1L, 1L, Role.USER)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void disableUser_revokesTokens() {
        User user = TestData.user(2L, "bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThat(userService.updateStatus(1L, 2L, false).enabled()).isFalse();
        verify(refreshTokenService).revokeAllForUser(2L);
    }

    @Test
    void enableUser_doesNotRevokeTokens() {
        User user = TestData.user(2L, "bob");
        user.setEnabled(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        userService.updateStatus(1L, 2L, true);

        verify(refreshTokenService, never()).revokeAllForUser(2L);
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
