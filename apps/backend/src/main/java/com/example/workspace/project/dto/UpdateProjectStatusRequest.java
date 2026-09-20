package com.example.workspace.project.dto;

import com.example.workspace.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateProjectStatusRequest", description = "仅更新项目状态")
public record UpdateProjectStatusRequest(
        @Schema(description = "目标状态")
        @NotNull ProjectStatus status,
        @Schema(description = "当前项目版本")
        @NotNull Long version
) {
}
