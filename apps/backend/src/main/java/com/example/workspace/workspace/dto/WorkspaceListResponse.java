package com.example.workspace.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "WorkspaceListResponse", description = "可访问的工作空间列表")
public record WorkspaceListResponse(
        @Schema(description = "工作空间") List<WorkspaceDetailResponse> items
) {
}
