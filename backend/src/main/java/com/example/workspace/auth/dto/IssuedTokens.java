package com.example.workspace.auth.dto;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static IssuedTokens bearer(String accessToken, String refreshToken, long expiresIn) {
        return new IssuedTokens(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
