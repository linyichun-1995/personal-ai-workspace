package com.example.workspace.auth.controller;

import com.example.workspace.auth.application.AuthService;
import com.example.workspace.auth.application.AuthSession;
import com.example.workspace.auth.dto.AuthResponse;
import com.example.workspace.auth.dto.LoginRequest;
import com.example.workspace.auth.dto.RegisterRequest;
import com.example.workspace.common.exception.UnauthorizedException;
import com.example.workspace.infrastructure.security.RefreshTokenCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return writeSession(authService.register(request), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return writeSession(authService.login(request), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.read(request)
                .orElseThrow(UnauthorizedException::refreshTokenInvalid);
        return writeSession(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenCookieService.read(request).ifPresent(authService::logout);
        refreshTokenCookieService.clear(response);
    }

    private AuthResponse writeSession(AuthSession session, HttpServletResponse response) {
        refreshTokenCookieService.write(response, session.refreshToken());
        return session.body();
    }
}
