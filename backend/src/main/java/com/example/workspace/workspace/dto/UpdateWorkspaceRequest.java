package com.example.workspace.workspace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 50) String timezone,
        @Min(1) @Max(7) int weekStartsOn,
        @NotNull Long version
) {
}
