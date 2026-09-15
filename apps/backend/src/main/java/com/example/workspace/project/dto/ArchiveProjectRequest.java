package com.example.workspace.project.dto;

import jakarta.validation.constraints.NotNull;

public record ArchiveProjectRequest(@NotNull Long version) {
}
