package com.example.workspace.user.dto;

import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name
) {
}
