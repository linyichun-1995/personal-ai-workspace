package com.example.workspace.task.dto;

import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "CreateTaskRequest", description = "创建任务")
public record CreateTaskRequest(
        @Schema(description = "所属项目，可空") UUID projectId,
        @Schema(description = "父任务 ID。V0.1 仅支持一级子任务") UUID parentId,
        @Schema(description = "标题", example = "补齐 OpenAPI 文档")
        @NotBlank @Size(max = 300) String title,
        @Schema(description = "描述")
        @Size(max = 20_000) String description,
        @Schema(description = "初始状态，省略时由服务端默认") TaskStatus status,
        @Schema(description = "优先级，省略时由服务端默认") TaskPriority priority,
        @Schema(description = "开始时间") Instant startAt,
        @Schema(description = "截止时间，不能早于开始时间") Instant dueAt
) {
}
