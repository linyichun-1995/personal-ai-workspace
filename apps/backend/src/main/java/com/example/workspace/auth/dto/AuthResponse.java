package com.example.workspace.auth.dto;

import com.example.workspace.user.dto.UserResponse;
import com.example.workspace.user.dto.WorkspaceResponse;

public record AuthResponse(
        UserResponse user,
        WorkspaceResponse workspace,
        String accessToken,
        String tokenType,
        long expiresIn
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
