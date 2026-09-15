package com.example.workspace.project.dto;

import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20_000) String description,
        ProjectStatus status,
        ProjectPriority priority,
        LocalDate startDate,
        LocalDate dueDate
) {
}
