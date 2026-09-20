package com.example.workspace.project.dto;

import com.example.workspace.project.domain.Project;
import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "ProjectResponse", description = "项目详情，含任务/笔记统计")
public record ProjectResponse(
        @Schema(description = "项目 ID") UUID id,
        @Schema(description = "所属工作空间 ID") UUID workspaceId,
        @Schema(description = "名称") String name,
        @Schema(description = "描述") String description,
        @Schema(description = "状态") ProjectStatus status,
        @Schema(description = "优先级") ProjectPriority priority,
        @Schema(description = "开始日期") LocalDate startDate,
        @Schema(description = "截止日期") LocalDate dueDate,
        @Schema(description = "归档时间，未归档时为 null") Instant archivedAt,
        @Schema(description = "创建时间") Instant createdAt,
        @Schema(description = "更新时间") Instant updatedAt,
        @Schema(description = "乐观锁版本") long version,
        @Schema(description = "任务与笔记统计") ProjectStatsResponse stats
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
