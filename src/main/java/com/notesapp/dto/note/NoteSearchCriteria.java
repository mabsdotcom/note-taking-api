package com.notesapp.dto.note;

import java.time.LocalDate;
import java.util.List;

import com.notesapp.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Optional query parameters for GET /api/notes and /api/notes/export.
 * Example: /api/notes?q=meeting&tags=work&tags=urgent&createdFrom=2026-01-01&sort=title,asc
 */
@ValidDateRange(from = "createdFrom", to = "createdTo")
@ValidDateRange(from = "updatedFrom", to = "updatedTo")
public record NoteSearchCriteria(
        @Parameter(description = "Text to search for in title and body (case-insensitive)")
        @Size(max = 100) String q,

        @Parameter(description = "Only notes in this category")
        Long categoryId,

        @Parameter(description = "Notes having ANY of these tags (repeat the parameter or use commas)")
        List<String> tags,

        @Parameter(description = "Filter by pinned state")
        Boolean pinned,

        @Parameter(description = "true = only archived notes; default shows only non-archived notes")
        Boolean archived,

        @Parameter(description = "Created on/after (yyyy-MM-dd, UTC)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,

        @Parameter(description = "Created on/before (yyyy-MM-dd, UTC)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,

        @Parameter(description = "Updated on/after (yyyy-MM-dd, UTC)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedFrom,

        @Parameter(description = "Updated on/before (yyyy-MM-dd, UTC)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedTo) {
}
