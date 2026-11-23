package com.notesapp.dto.note;

import java.time.Instant;

import com.notesapp.entity.NoteAction;

public record NoteHistoryResponse(
        NoteAction action,
        String titleSnapshot,
        String performedBy,
        Instant performedAt) {
}
