package com.example.workspace.common.domain;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

/** Generated schema bindings shared by the four searchable, taggable resource kinds. */
public record SourceTables(Table<? extends Record> table, Field<UUID> workspaceId, Field<UUID> id,
        Field<UUID> projectId, Field<String> title, Field<String> body, Field<Long> version,
        Field<Long> tagVersion, Field<OffsetDateTime> updatedAt, Field<OffsetDateTime> deletedAt,
        Condition stored, Condition archived, Tags tags) {
    public record Tags(Table<? extends Record> table, Field<UUID> workspaceId,
                       Field<UUID> sourceId, Field<UUID> tagId) {}

    public static SourceTables of(SourceType type) {
        return switch (type) {
            case PROJECT -> new SourceTables(PROJECTS, PROJECTS.WORKSPACE_ID, PROJECTS.ID, PROJECTS.ID,
                    PROJECTS.NAME, PROJECTS.DESCRIPTION, PROJECTS.VERSION, PROJECTS.TAG_VERSION,
                    PROJECTS.UPDATED_AT, PROJECTS.DELETED_AT, trueCondition(), PROJECTS.ARCHIVED_AT.isNotNull(),
                    new Tags(PROJECT_TAGS, PROJECT_TAGS.WORKSPACE_ID, PROJECT_TAGS.SOURCE_ID, PROJECT_TAGS.TAG_ID));
            case TASK -> new SourceTables(TASKS, TASKS.WORKSPACE_ID, TASKS.ID, TASKS.PROJECT_ID,
                    TASKS.TITLE, TASKS.DESCRIPTION, TASKS.VERSION, TASKS.TAG_VERSION,
                    TASKS.UPDATED_AT, TASKS.DELETED_AT, trueCondition(), falseCondition(),
                    new Tags(TASK_TAGS, TASK_TAGS.WORKSPACE_ID, TASK_TAGS.SOURCE_ID, TASK_TAGS.TAG_ID));
            case NOTE -> new SourceTables(NOTES, NOTES.WORKSPACE_ID, NOTES.ID, NOTES.PROJECT_ID,
                    NOTES.TITLE, NOTES.CONTENT, NOTES.VERSION, NOTES.TAG_VERSION,
                    NOTES.UPDATED_AT, NOTES.DELETED_AT, trueCondition(), NOTES.ARCHIVED.isTrue(),
                    new Tags(NOTE_TAGS, NOTE_TAGS.WORKSPACE_ID, NOTE_TAGS.SOURCE_ID, NOTE_TAGS.TAG_ID));
            case FILE -> new SourceTables(FILES, FILES.WORKSPACE_ID, FILES.ID, FILES.PROJECT_ID,
                    FILES.DISPLAY_NAME, inline(""), FILES.VERSION, FILES.TAG_VERSION,
                    FILES.UPDATED_AT, FILES.DELETED_AT, FILES.STATE.eq("STORED"), falseCondition(),
                    new Tags(FILE_TAGS, FILE_TAGS.WORKSPACE_ID, FILE_TAGS.SOURCE_ID, FILE_TAGS.TAG_ID));
        };
    }

    public Condition identity(UUID workspace, UUID source) {
        return workspaceId.eq(workspace).and(id.eq(source));
    }

    public Condition readable(SourceType type) {
        var parent = PROJECTS.as("source_parent");
        Condition visible = deletedAt.isNull().and(stored);
        return type == SourceType.PROJECT ? visible : visible.and(projectId.isNull().or(exists(
                selectOne().from(parent).where(parent.WORKSPACE_ID.eq(workspaceId)
                        .and(parent.ID.eq(projectId)).and(parent.DELETED_AT.isNull())))));
    }
}
