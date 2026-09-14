package com.example.workspace.user.controller;

import com.example.workspace.auth.application.AuthSession;
import com.example.workspace.auth.dto.AuthResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.security.RefreshTokenCookieService;
import com.example.workspace.user.application.UserService;
import com.example.workspace.user.dto.ChangePasswordRequest;
import com.example.workspace.user.dto.CurrentUserResponse;
import com.example.workspace.user.dto.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public UserController(UserService userService, RefreshTokenCookieService refreshTokenCookieService) {
        this.userService = userService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(CurrentUser currentUser) {
        return userService.me(currentUser);
    }

    @PatchMapping("/me")
    public CurrentUserResponse updateMe(CurrentUser currentUser, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser, request);
    }

    @PutMapping("/me/password")
    public AuthResponse changePassword(
            CurrentUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response
    ) {
        AuthSession session = userService.changePassword(currentUser, request);
        refreshTokenCookieService.write(response, session.refreshToken());
        return session.body();
    }
}
