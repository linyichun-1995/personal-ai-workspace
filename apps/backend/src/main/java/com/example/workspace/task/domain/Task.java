package com.example.workspace.task.domain;

import java.time.Instant;
import java.util.UUID;

public record Task(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID parentId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant startAt,
        Instant dueAt,
        Instant completedAt,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {
}
