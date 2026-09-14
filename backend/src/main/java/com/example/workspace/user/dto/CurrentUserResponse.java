package com.example.workspace.user.dto;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        String locale,
        String timezone,
        long version,
        WorkspaceResponse currentWorkspace
) {
}
