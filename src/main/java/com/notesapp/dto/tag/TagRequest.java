package com.notesapp.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(@Schema(example = "urgent") @NotBlank @Size(max = 50) String name) {
}
