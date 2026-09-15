package com.example.workspace.task.dto;

import com.example.workspace.task.domain.Task;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        String projectName,
        UUID parentId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant startAt,
        Instant dueAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static TaskResponse from(Task task, String projectName) {
        return new TaskResponse(
                task.id(),
                task.workspaceId(),
                task.projectId(),
                projectName,
                task.parentId(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.startAt(),
                task.dueAt(),
                task.completedAt(),
                task.createdAt(),
                task.updatedAt(),
                task.version()
        );
    }
}
