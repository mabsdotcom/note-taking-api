package com.notesapp.service;

import java.util.Set;

import com.notesapp.dto.common.PageResponse;
import com.notesapp.dto.user.UserResponse;
import com.notesapp.entity.Role;
import com.notesapp.entity.User;
import com.notesapp.exception.BadRequestException;
import com.notesapp.exception.ResourceNotFoundException;
import com.notesapp.mapper.UserMapper;
import com.notesapp.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private static final Set<String> SORTABLE = Set.of("id", "username", "email", "createdAt", "role");

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserMapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        SortValidator.validate(pageable, SORTABLE);
        return PageResponse.from(userRepository.findAll(pageable).map(UserMapper::toResponse));
    }

    public UserResponse updateRole(Long actingAdminId, Long userId, Role role) {
        if (actingAdminId.equals(userId)) {
            throw new BadRequestException("You cannot change your own role");
        }
        User user = find(userId);
        user.setRole(role);
        return UserMapper.toResponse(user);
    }

    /** Disabling a user also revokes all of their refresh tokens. */
    public UserResponse updateStatus(Long actingAdminId, Long userId, boolean enabled) {
        if (actingAdminId.equals(userId)) {
            throw new BadRequestException("You cannot disable your own account");
        }
        User user = find(userId);
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenService.revokeAllForUser(userId);
        }
        return UserMapper.toResponse(user);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
