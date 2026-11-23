package com.notesapp.controller;

import com.notesapp.dto.user.UserResponse;
import com.notesapp.security.AppUserPrincipal;
import com.notesapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "The current user's profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the profile of the logged-in user")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getById(principal.getId());
    }
}
