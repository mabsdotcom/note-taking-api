package com.notesapp.dto.note;

import java.time.Instant;
import java.util.List;

import com.notesapp.dto.category.CategorySummary;

/** Lightweight representation for list endpoints: an excerpt instead of the full HTML body. */
public record NoteSummaryResponse(
        Long id,
        String title,
        String excerpt,
        boolean pinned,
        boolean archived,
        CategorySummary category,
        List<String> tags,
        String owner,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
}
