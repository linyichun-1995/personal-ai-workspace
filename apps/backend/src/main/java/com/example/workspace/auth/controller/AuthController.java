package com.example.workspace.auth.controller;

import com.example.workspace.auth.application.AuthService;
import com.example.workspace.auth.application.AuthSession;
import com.example.workspace.auth.dto.AuthResponse;
import com.example.workspace.auth.dto.LoginRequest;
import com.example.workspace.auth.dto.RegisterRequest;
import com.example.workspace.common.exception.UnauthorizedException;
import com.example.workspace.infrastructure.config.OpenApiConfig;
import com.example.workspace.infrastructure.security.RefreshTokenCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "注册", description = "创建账号并自动创建默认 Workspace。Access Token 在 JSON 返回；Refresh Token 写入 HttpOnly Cookie `refresh_token`。")
    @ApiResponse(
            responseCode = "201",
            description = "注册成功",
            headers = @Header(name = "Set-Cookie", description = "下发 `refresh_token` HttpOnly Cookie"),
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "409", description = "邮箱已注册")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return writeSession(authService.register(request), response);
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "校验邮箱与密码。成功后返回 Access Token，并轮换写入 Refresh Cookie。同一 IP 默认 1 分钟内最多 10 次。")
    @ApiResponse(
            responseCode = "200",
            description = "登录成功",
            headers = @Header(name = "Set-Cookie", description = "下发 `refresh_token` HttpOnly Cookie"),
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "邮箱或密码错误，或账号已禁用")
    @ApiResponse(responseCode = "429", description = "登录过于频繁")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return writeSession(authService.login(request), response);
    }

    @PostMapping("/refresh")
    @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE)
    @Operation(summary = "刷新令牌", description = "读取 Cookie `refresh_token`，轮换 Refresh Token 并签发新的 Access Token。")
    @ApiResponse(
            responseCode = "200",
            description = "刷新成功",
            headers = @Header(name = "Set-Cookie", description = "轮换后的 `refresh_token` HttpOnly Cookie"),
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "Refresh Token 缺失、无效或已过期")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.read(request)
                .orElseThrow(UnauthorizedException::refreshTokenInvalid);
        return writeSession(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE)
    @Operation(summary = "退出登录", description = "撤销当前 Refresh Token family（若 Cookie 存在）并清除 Cookie。无需 Access Token。")
    @ApiResponse(responseCode = "204", description = "已退出", content = @Content)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenCookieService.read(request).ifPresent(authService::logout);
        refreshTokenCookieService.clear(response);
    }

    private AuthResponse writeSession(AuthSession session, HttpServletResponse response) {
        refreshTokenCookieService.write(response, session.refreshToken());
        return session.body();
    }
}
