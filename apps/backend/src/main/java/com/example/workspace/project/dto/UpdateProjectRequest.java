package com.example.workspace.project.dto;

import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(name = "UpdateProjectRequest", description = "全量更新项目")
public record UpdateProjectRequest(
        @Schema(description = "项目名称")
        @NotBlank @Size(max = 200) String name,
        @Schema(description = "项目描述")
        @Size(max = 20_000) String description,
        @Schema(description = "项目状态")
        @NotNull ProjectStatus status,
        @Schema(description = "优先级")
        @NotNull ProjectPriority priority,
        @Schema(description = "开始日期") LocalDate startDate,
        @Schema(description = "截止日期，不能早于开始日期") LocalDate dueDate,
        @Schema(description = "当前项目版本")
        @NotNull Long version
) {
}
