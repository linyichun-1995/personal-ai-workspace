package com.example.workspace.note.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateNoteRequest(
        UUID projectId,
        @Size(max = 300) String title,
        @Size(max = 200_000) String content,
        Boolean favorite,
        Boolean archived,
        @NotNull Long version
) {
}
