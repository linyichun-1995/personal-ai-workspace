package com.example.workspace.note.domain;

import java.time.Instant;
import java.util.UUID;

public record Note(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        String title,
        String content,
        String summary,
        boolean favorite,
        boolean archived,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {
}
