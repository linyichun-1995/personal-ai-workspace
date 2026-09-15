package com.example.workspace.workspace.domain;

import java.time.Instant;
import java.util.UUID;

public record Workspace(
        UUID id,
        String name,
        String slug,
        WorkspaceType type,
        String timezone,
        int weekStartsOn,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
