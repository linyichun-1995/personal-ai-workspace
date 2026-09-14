package com.example.workspace.common.security;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String email
) {
}
