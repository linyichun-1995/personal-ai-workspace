package com.example.workspace.common.security;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;

@Hidden
public record CurrentUser(
        UUID userId,
        String email
) {
}
