package com.example.workspace.user.controller;

import com.example.workspace.auth.application.AuthSession;
import com.example.workspace.auth.dto.AuthResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.infrastructure.security.RefreshTokenCookieService;
import com.example.workspace.user.application.UserService;
import com.example.workspace.user.dto.ChangePasswordRequest;
import com.example.workspace.user.dto.CurrentUserResponse;
import com.example.workspace.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Current User")
@AuthenticatedApi
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public UserController(UserService userService, RefreshTokenCookieService refreshTokenCookieService) {
        this.userService = userService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户", description = "返回资料，以及当前默认 Workspace。")
    @ApiResponse(responseCode = "200", description = "当前用户", content = @Content(schema = @Schema(implementation = CurrentUserResponse.class)))
    public CurrentUserResponse me(CurrentUser currentUser) {
        return userService.me(currentUser);
    }

    @PatchMapping("/me")
    @Operation(summary = "更新资料", description = "更新显示名、头像、语言和时区。需要携带当前 `version`。")
    @ApiResponse(responseCode = "200", description = "更新后的用户", content = @Content(schema = @Schema(implementation = CurrentUserResponse.class)))
    @ApiResponse(responseCode = "409", description = "资料已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "时区无效")
    public CurrentUserResponse updateMe(CurrentUser currentUser, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser, request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改密码", description = "校验当前密码后改密。会使旧 Access Token 立即失效，并撤销其他会话；当前会话会签发新的 Access Token 并轮换 Refresh Cookie。")
    @ApiResponse(
            responseCode = "200",
            description = "改密成功，返回新的 Access Token",
            headers = @Header(name = "Set-Cookie", description = "轮换后的 `refresh_token` HttpOnly Cookie"),
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "当前密码错误")
    @ApiResponse(responseCode = "422", description = "新密码与当前密码相同")
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
