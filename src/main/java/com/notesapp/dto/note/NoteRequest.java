package com.notesapp.dto.note;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used for both create (POST) and full update (PUT). */
public record NoteRequest(
        @Schema(example = "Sprint planning")
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "Rich text as HTML. Unsafe tags/attributes (script, onclick, ...) are removed.",
                example = "<h2>Agenda</h2><ul><li><b>Review</b> backlog</li><li>Estimate stories</li></ul>")
        @Size(max = 100_000) String content,

        @Schema(description = "Optional id of one of your categories", example = "1")
        Long categoryId,

        @Schema(description = "Tag names; missing tags are created automatically", example = "[\"work\", \"meeting\"]")
        @Size(max = 20, message = "a note can have at most 20 tags")
        List<@NotBlank @Size(max = 50) String> tags,

        @Schema(example = "false") Boolean pinned,

        @Schema(example = "false") Boolean archived) {
}
