package com.example.workspace.project.dto;

import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20_000) String description,
        @NotNull ProjectStatus status,
        @NotNull ProjectPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        @NotNull Long version
) {
}
