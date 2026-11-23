package com.notesapp.dto.auth;

import com.notesapp.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "jane_doe")
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may only contain letters, digits, '_', '.' and '-'")
        String username,

        @Schema(example = "jane@example.com")
        @NotBlank @Email @Size(max = 100)
        String email,

        @Schema(example = "Secret@123")
        @NotBlank @StrongPassword
        String password) {
}
