package com.notesapp.dto.user;

import com.notesapp.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@Schema(example = "ADMIN") @NotNull Role role) {
}
