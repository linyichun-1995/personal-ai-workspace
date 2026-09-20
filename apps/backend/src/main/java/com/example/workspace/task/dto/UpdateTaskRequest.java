package com.example.workspace.task.dto;

import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "UpdateTaskRequest", description = "全量更新任务")
public record UpdateTaskRequest(
        @Schema(description = "所属项目，可空") UUID projectId,
        @Schema(description = "父任务 ID") UUID parentId,
        @Schema(description = "标题")
        @NotBlank @Size(max = 300) String title,
        @Schema(description = "描述")
        @Size(max = 20_000) String description,
        @Schema(description = "状态")
        @NotNull TaskStatus status,
        @Schema(description = "优先级")
        @NotNull TaskPriority priority,
        @Schema(description = "开始时间") Instant startAt,
        @Schema(description = "截止时间，不能早于开始时间") Instant dueAt,
        @Schema(description = "当前任务版本")
        @NotNull Long version
) {
}
