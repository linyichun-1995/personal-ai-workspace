package com.example.workspace.project.dto;

import com.example.workspace.project.domain.Project;
import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String description,
        ProjectStatus status,
        ProjectPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        ProjectStatsResponse stats
) {
    public static ProjectResponse from(Project project, ProjectStatsResponse stats) {
        return new ProjectResponse(
                project.id(),
                project.workspaceId(),
                project.name(),
                project.description(),
                project.status(),
                project.priority(),
                project.startDate(),
                project.dueDate(),
                project.archivedAt(),
                project.createdAt(),
                project.updatedAt(),
                project.version(),
                stats
        );
    }
}
