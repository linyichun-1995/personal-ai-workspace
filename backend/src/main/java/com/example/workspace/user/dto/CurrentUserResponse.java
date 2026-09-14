package com.example.workspace.user.dto;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        WorkspaceResponse currentWorkspace
) {
}
