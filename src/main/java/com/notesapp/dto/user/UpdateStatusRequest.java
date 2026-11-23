package com.notesapp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@Schema(example = "false") @NotNull Boolean enabled) {
}
