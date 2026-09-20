package com.example.workspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "WorkspaceResponse", description = "工作空间摘要")
public record WorkspaceResponse(
        @Schema(description = "工作空间 ID") UUID id,
        @Schema(description = "工作空间名称", example = "我的工作空间") String name
) {
}
