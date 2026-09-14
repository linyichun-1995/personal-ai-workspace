package com.example.workspace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 1000) String avatarUrl,
        @NotBlank @Size(max = 20) String locale,
        @NotBlank @Size(max = 50) String timezone,
        @NotNull Long version
) {
}
