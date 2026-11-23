package com.notesapp.controller;

import java.util.List;

import com.notesapp.dto.tag.TagRequest;
import com.notesapp.dto.tag.TagResponse;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Manage tags (tags are also created automatically when saving notes)")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "List your tags")
    public List<TagResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return tagService.list(principal.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tag")
    public TagResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return tagService.get(principal.getId(), id);
    }

    @PostMapping
    @Operation(summary = "Create a tag")
    public ResponseEntity<TagResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a tag")
    public TagResponse rename(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
                              @Valid @RequestBody TagRequest request) {
        return tagService.rename(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag and remove it from all notes")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        tagService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
