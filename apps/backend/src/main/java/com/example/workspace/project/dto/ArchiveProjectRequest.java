package com.example.workspace.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ArchiveProjectRequest", description = "归档或恢复项目时提交当前版本")
public record ArchiveProjectRequest(
        @Schema(description = "当前项目版本")
        @NotNull Long version
) {
}
