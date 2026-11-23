package com.notesapp.dto.category;

/** Compact category embedded inside note responses. */
public record CategorySummary(Long id, String name, String color) {
}
