package com.example.workspace.common.security;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UUID workspaceId,
        String email
) {
}
