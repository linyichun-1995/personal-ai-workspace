package com.example.workspace.note.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateNoteRequest(
        UUID projectId,
        @Size(max = 300) String title,
        @Size(max = 200_000) String content
) {
}
