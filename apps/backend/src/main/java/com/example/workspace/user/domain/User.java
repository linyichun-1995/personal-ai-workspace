package com.example.workspace.user.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String emailNormalized,
        String passwordHash,
        String displayName,
        String avatarUrl,
        String locale,
        String timezone,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt,
        long version
) {
}
