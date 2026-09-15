package com.example.workspace.project.dto;

import com.example.workspace.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectStatusRequest(
        @NotNull ProjectStatus status,
        @NotNull Long version
) {
}
