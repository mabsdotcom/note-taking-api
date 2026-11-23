package com.notesapp.dto.tag;

import java.time.Instant;

public record TagResponse(Long id, String name, Instant createdAt) {
}
