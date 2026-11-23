package com.notesapp.dto.note;

import java.time.Instant;
import java.util.List;

import com.notesapp.dto.category.CategorySummary;

public record NoteResponse(
        Long id,
        String title,
        String content,
        boolean pinned,
        boolean archived,
        CategorySummary category,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {
}
