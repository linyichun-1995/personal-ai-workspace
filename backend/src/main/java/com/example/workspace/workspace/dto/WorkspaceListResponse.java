package com.example.workspace.workspace.dto;

import java.util.List;

public record WorkspaceListResponse(
        List<WorkspaceDetailResponse> items
) {
}
