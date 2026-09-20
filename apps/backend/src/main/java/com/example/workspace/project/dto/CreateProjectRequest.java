package com.example.workspace.project.dto;

import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(name = "CreateProjectRequest", description = "创建项目")
public record CreateProjectRequest(
        @Schema(description = "项目名称", example = "V0.1 工作台")
        @NotBlank @Size(max = 200) String name,
        @Schema(description = "项目描述")
        @Size(max = 20_000) String description,
        @Schema(description = "初始状态，省略时由服务端默认") ProjectStatus status,
        @Schema(description = "优先级，省略时由服务端默认") ProjectPriority priority,
        @Schema(description = "开始日期", example = "2026-09-01") LocalDate startDate,
        @Schema(description = "截止日期，不能早于开始日期", example = "2026-09-30") LocalDate dueDate
) {
}
