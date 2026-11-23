package com.notesapp.mapper;

import com.notesapp.dto.category.CategoryResponse;
import com.notesapp.dto.category.CategorySummary;
import com.notesapp.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category c, long noteCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getColor(),
                noteCount, c.getCreatedAt(), c.getUpdatedAt());
    }

    public static CategorySummary toSummary(Category c) {
        return c == null ? null : new CategorySummary(c.getId(), c.getName(), c.getColor());
    }
}
