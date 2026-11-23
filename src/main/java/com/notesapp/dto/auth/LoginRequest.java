package com.notesapp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Username or email", example = "demo")
        @NotBlank String username,

        @Schema(example = "Demo@1234")
        @NotBlank String password) {
}
