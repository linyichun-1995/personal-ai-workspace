package com.example.workspace.user.dto;

import java.util.UUID;

public record WorkspaceSummaryResponse(
        UUID id,
        String name,
        String slug,
        String timezone
) {
}
