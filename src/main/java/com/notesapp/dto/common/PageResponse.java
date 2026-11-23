package com.notesapp.dto.common;

import java.util.List;

import org.springframework.data.domain.Page;

/** Stable pagination envelope (instead of serializing Spring's Page directly). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getSort().isSorted() ? page.getSort().toString() : null);
    }
}
