package com.notesapp.mapper;

import com.notesapp.dto.tag.TagResponse;
import com.notesapp.entity.Tag;

public final class TagMapper {

    private TagMapper() {
    }

    public static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt());
    }
}
