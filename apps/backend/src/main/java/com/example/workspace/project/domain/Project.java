package com.example.workspace.project.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Project(
        UUID id,
        UUID workspaceId,
        String name,
        String description,
        ProjectStatus status,
        ProjectPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        Instant archivedAt,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {
    public boolean archived() {
        return archivedAt != null;
    }
}
