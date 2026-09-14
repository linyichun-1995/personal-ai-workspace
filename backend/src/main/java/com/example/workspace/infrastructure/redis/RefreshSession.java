package com.example.workspace.infrastructure.redis;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID userId,
        String email,
        UUID familyId,
        Instant expiresAt,
        boolean revoked
) {
    public RefreshSession revoke() {
        return new RefreshSession(userId, email, familyId, expiresAt, true);
    }

    public RefreshSession expireAt(Instant expiresAt) {
        return new RefreshSession(userId, email, familyId, expiresAt, revoked);
    }

    public boolean expired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
