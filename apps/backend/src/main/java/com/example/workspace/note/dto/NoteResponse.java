package com.example.workspace.note.dto;

import com.example.workspace.note.domain.Note;
import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        String title,
        String content,
        String summary,
        boolean favorite,
        boolean archived,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static NoteResponse from(Note note, boolean includeContent) {
        return new NoteResponse(
                note.id(),
                note.workspaceId(),
                note.projectId(),
                note.title(),
                includeContent ? note.content() : null,
                note.summary(),
                note.favorite(),
                note.archived(),
                note.createdAt(),
                note.updatedAt(),
                note.version()
        );
    }
}
