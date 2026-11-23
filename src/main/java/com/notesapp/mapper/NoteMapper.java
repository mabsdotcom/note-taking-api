package com.notesapp.mapper;

import java.util.List;

import com.notesapp.dto.note.NoteHistoryResponse;
import com.notesapp.dto.note.NoteResponse;
import com.notesapp.dto.note.NoteSummaryResponse;
import com.notesapp.entity.Note;
import com.notesapp.entity.NoteHistory;
import com.notesapp.entity.Tag;

/** Entity -> DTO conversion. Entities are never returned from controllers. */
public final class NoteMapper {

    static final int EXCERPT_LENGTH = 150;

    private NoteMapper() {
    }

    public static NoteResponse toResponse(Note n) {
        return new NoteResponse(n.getId(), n.getTitle(), n.getContent(), n.isPinned(), n.isArchived(),
                CategoryMapper.toSummary(n.getCategory()), tagNames(n),
                n.getCreatedAt(), n.getUpdatedAt(), n.getCreatedBy(), n.getUpdatedBy());
    }

    public static NoteSummaryResponse toSummary(Note n) {
        return new NoteSummaryResponse(n.getId(), n.getTitle(), excerpt(n.getPlainText()), n.isPinned(),
                n.isArchived(), CategoryMapper.toSummary(n.getCategory()), tagNames(n),
                n.getOwner() == null ? null : n.getOwner().getUsername(),
                n.getCreatedAt(), n.getUpdatedAt(), n.getDeletedAt());
    }

    public static NoteHistoryResponse toHistory(NoteHistory h) {
        return new NoteHistoryResponse(h.getAction(), h.getTitleSnapshot(), h.getPerformedBy(), h.getPerformedAt());
    }

    static String excerpt(String plainText) {
        if (plainText == null) {
            return "";
        }
        String trimmed = plainText.strip();
        return trimmed.length() <= EXCERPT_LENGTH ? trimmed : trimmed.substring(0, EXCERPT_LENGTH).strip() + "...";
    }

    private static List<String> tagNames(Note n) {
        return n.getTags().stream()
                .map(Tag::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
