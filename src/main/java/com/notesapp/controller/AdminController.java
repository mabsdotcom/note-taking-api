package com.notesapp.controller;

import com.notesapp.dto.common.PageResponse;
import com.notesapp.dto.note.NoteResponse;
import com.notesapp.dto.note.NoteSummaryResponse;
import com.notesapp.dto.user.UpdateRoleRequest;
import com.notesapp.dto.user.UpdateStatusRequest;
import com.notesapp.dto.user.UserResponse;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.service.NoteService;
import com.notesapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-only endpoints: secured by URL rule in SecurityConfig and by @PreAuthorize. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "User management and trash recovery (role ADMIN)")
@ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
public class AdminController {

    private final UserService userService;
    private final NoteService noteService;

    public AdminController(UserService userService, NoteService noteService) {
        this.userService = userService;
        this.noteService = noteService;
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "OK")
    public PageResponse<UserResponse> users(@ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.list(pageable);
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change a user's role")
    @ApiResponse(responseCode = "200", description = "OK")
    public UserResponse updateRole(@AuthenticationPrincipal AppUserPrincipal admin, @PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(admin.getId(), id, request.role());
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Enable or disable a user account (disabling revokes their tokens)")
    @ApiResponse(responseCode = "200", description = "OK")
    public UserResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal admin, @PathVariable Long id,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return userService.updateStatus(admin.getId(), id, request.enabled());
    }

    @GetMapping("/notes/deleted")
    @Operation(summary = "List soft-deleted notes of all users")
    @ApiResponse(responseCode = "200", description = "OK")
    public PageResponse<NoteSummaryResponse> deletedNotes(
            @ParameterObject @PageableDefault(size = 20, sort = "deletedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return noteService.listAllDeleted(pageable);
    }

    @PostMapping("/notes/{id}/restore")
    @Operation(summary = "Restore any user's soft-deleted note")
    @ApiResponse(responseCode = "200", description = "OK")
    public NoteResponse restoreNote(@PathVariable Long id) {
        return noteService.adminRestore(id);
    }
}
