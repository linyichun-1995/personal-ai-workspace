package com.example.workspace.user.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl
) {
}
