package com.example.workspace.workspace.dto;

import com.example.workspace.workspace.application.WorkspaceAccess;
import java.util.UUID;

public record WorkspaceDetailResponse(
        UUID id,
        String name,
        String timezone,
        int weekStartsOn,
        String role,
        long version
) {
    public static WorkspaceDetailResponse from(WorkspaceAccess access) {
        return new WorkspaceDetailResponse(
                access.workspace().id(),
                access.workspace().name(),
                access.workspace().timezone(),
                access.workspace().weekStartsOn(),
                access.member().role().name(),
                access.workspace().version()
        );
    }
}
