package com.notesapp.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.notesapp.dto.common.ErrorResponse;
import com.notesapp.dto.common.PageResponse;
import com.notesapp.dto.note.NoteHistoryResponse;
import com.notesapp.dto.note.NoteRequest;
import com.notesapp.dto.note.NoteResponse;
import com.notesapp.dto.note.NoteSearchCriteria;
import com.notesapp.dto.note.NoteSummaryResponse;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/notes")
@Tag(name = "Notes", description = "Rich text notes: CRUD, search & filtering, trash, history and CSV export")
@ApiResponse(responseCode = "401", description = "Missing or invalid token",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    @Operation(summary = "Search and filter your notes (paginated)",
            description = "All filters are optional and combined with AND. Sortable fields: "
                    + "title, createdAt, updatedAt, pinned. Default sort: pinned first, then most recently updated. "
                    + "Example: ?q=meeting&tags=work&createdFrom=2026-01-01&sort=title,asc&page=0&size=10")
    @ApiResponse(responseCode = "200", description = "OK")
    public PageResponse<NoteSummaryResponse> search(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @ParameterObject NoteSearchCriteria criteria,
            @ParameterObject
            @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "pinned", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC)})
            Pageable pageable) {
        return noteService.search(principal.getId(), criteria, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one note including its full HTML content")
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "200", description = "OK")
    public NoteResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return noteService.getById(principal.getId(), id);
    }

    @PostMapping
    @Operation(summary = "Create a note", description = "HTML content is sanitized; unknown tags are created.")
    @ApiResponse(responseCode = "201", description = "Note created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NoteResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                               @Valid @RequestBody NoteRequest request) {
        NoteResponse created = noteService.create(principal.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a note (title, content, category, tags, flags)")
    @ApiResponse(responseCode = "200", description = "OK")
    public NoteResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
                               @Valid @RequestBody NoteRequest request) {
        return noteService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Move a note to the trash (soft delete)")
    @ApiResponse(responseCode = "204", description = "Moved to trash")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        noteService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    @Operation(summary = "List notes in your trash")
    @ApiResponse(responseCode = "200", description = "OK")
    public PageResponse<NoteSummaryResponse> trash(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @ParameterObject @PageableDefault(size = 20, sort = "deletedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return noteService.trash(principal.getId(), pageable);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a note from the trash")
    @ApiResponse(responseCode = "200", description = "OK")
    public NoteResponse restore(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return noteService.restore(principal.getId(), id);
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Permanently delete a note (cannot be undone)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deletePermanently(@AuthenticationPrincipal AppUserPrincipal principal,
                                                  @PathVariable Long id) {
        noteService.deletePermanently(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Change history (audit trail) of a note")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<NoteHistoryResponse> history(@AuthenticationPrincipal AppUserPrincipal principal,
                                             @PathVariable Long id) {
        return noteService.history(principal.getId(), id);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export notes as CSV", description = "Accepts the same filters as the search endpoint.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal AppUserPrincipal principal,
                                            @Valid @ParameterObject NoteSearchCriteria criteria) {
        byte[] body = noteService.exportCsv(principal.getId(), criteria).getBytes(StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("notes-" + LocalDate.now() + ".csv")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
