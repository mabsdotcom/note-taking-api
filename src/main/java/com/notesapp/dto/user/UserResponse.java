package com.notesapp.dto.user;

import java.time.Instant;

import com.notesapp.entity.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt) {
}
