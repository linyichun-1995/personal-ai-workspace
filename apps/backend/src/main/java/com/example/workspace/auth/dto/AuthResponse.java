package com.example.workspace.auth.dto;

import com.example.workspace.user.dto.UserResponse;
import com.example.workspace.user.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "认证成功响应。Refresh Token 只走 Cookie，不出现在 JSON 中。")
public record AuthResponse(
        @Schema(description = "当前用户") UserResponse user,
        @Schema(description = "当前默认工作空间") WorkspaceResponse workspace,
        @Schema(description = "Access Token，放入 Authorization Bearer 头", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "令牌类型", example = "Bearer") String tokenType,
        @Schema(description = "Access Token 有效期（秒）", example = "900") long expiresIn
) {
    public static AuthResponse of(UserResponse user, WorkspaceResponse workspace, IssuedTokens tokens) {
        return new AuthResponse(
                user,
                workspace,
                tokens.accessToken(),
                tokens.tokenType(),
                tokens.expiresIn()
        );
    }
}
