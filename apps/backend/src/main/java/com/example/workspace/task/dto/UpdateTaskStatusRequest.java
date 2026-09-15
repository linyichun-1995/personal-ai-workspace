package com.example.workspace.task.dto;

import com.example.workspace.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status,
        @NotNull Long version
) {
}
