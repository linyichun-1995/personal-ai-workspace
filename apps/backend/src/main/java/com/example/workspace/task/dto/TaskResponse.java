package com.example.workspace.task.dto;

import com.example.workspace.task.domain.Task;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "TaskResponse", description = "任务详情")
public record TaskResponse(
        @Schema(description = "任务 ID") UUID id,
        @Schema(description = "所属工作空间 ID") UUID workspaceId,
        @Schema(description = "所属项目 ID，可空") UUID projectId,
        @Schema(description = "所属项目名称，无项目时为 null") String projectName,
        @Schema(description = "父任务 ID，顶级任务为 null") UUID parentId,
        @Schema(description = "标题") String title,
        @Schema(description = "描述") String description,
        @Schema(description = "状态") TaskStatus status,
        @Schema(description = "优先级") TaskPriority priority,
        @Schema(description = "开始时间") Instant startAt,
        @Schema(description = "截止时间") Instant dueAt,
        @Schema(description = "完成时间，未完成时为 null") Instant completedAt,
        @Schema(description = "创建时间") Instant createdAt,
        @Schema(description = "更新时间") Instant updatedAt,
        @Schema(description = "乐观锁版本") long version
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
