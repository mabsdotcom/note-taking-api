package com.notesapp.dto.category;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String color,
        long noteCount,
        Instant createdAt,
        Instant updatedAt) {
}
