package com.example.workspace.auth.application;

import com.example.workspace.auth.dto.AuthResponse;

public record AuthSession(
        AuthResponse body,
        String refreshToken
) {
}
