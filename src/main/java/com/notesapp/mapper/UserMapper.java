package com.notesapp.mapper;

import com.notesapp.dto.user.UserResponse;
import com.notesapp.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
