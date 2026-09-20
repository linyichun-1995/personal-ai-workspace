package com.example.workspace.task.dto;

import com.example.workspace.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateTaskStatusRequest", description = "仅更新任务状态")
public record UpdateTaskStatusRequest(
        @Schema(description = "目标状态")
        @NotNull TaskStatus status,
        @Schema(description = "当前任务版本")
        @NotNull Long version
) {
}
