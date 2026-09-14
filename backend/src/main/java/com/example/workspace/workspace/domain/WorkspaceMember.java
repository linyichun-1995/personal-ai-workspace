package com.example.workspace.workspace.domain;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMember(
        UUID id,
        UUID workspaceId,
        UUID userId,
        WorkspaceRole role,
        Instant joinedAt
) {
}
