package com.notesapp.dto.category;

import com.notesapp.validation.HexColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Schema(example = "Work")
        @NotBlank @Size(max = 50) String name,

        @Schema(example = "Meeting notes and project ideas")
        @Size(max = 255) String description,

        @Schema(example = "#1E88E5")
        @HexColor String color) {
}
