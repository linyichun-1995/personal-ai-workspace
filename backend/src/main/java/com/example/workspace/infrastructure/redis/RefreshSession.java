package com.example.workspace.infrastructure.redis;

import java.util.UUID;

public record RefreshSession(
        UUID userId,
        UUID workspaceId,
        String email,
        String familyId
) {
}
