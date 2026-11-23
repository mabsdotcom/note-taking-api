package com.notesapp.controller;

import java.net.URI;
import java.util.List;

import com.notesapp.dto.category.CategoryRequest;
import com.notesapp.dto.category.CategoryResponse;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Organise notes into categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List your categories with their note counts")
    public List<CategoryResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return categoryService.list(principal.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category")
    public CategoryResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return categoryService.get(principal.getId(), id);
    }

    @PostMapping
    @Operation(summary = "Create a category (name must be unique for you)")
    public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(principal.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public CategoryResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category; its notes become uncategorised")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        categoryService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
