package com.example.workspace.task.dto;

import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpdateTaskRequest(
        UUID projectId,
        UUID parentId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 20_000) String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        Instant startAt,
        Instant dueAt,
        @NotNull Long version
) {
}
