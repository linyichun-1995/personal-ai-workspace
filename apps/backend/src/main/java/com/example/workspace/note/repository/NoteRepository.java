package com.example.workspace.note.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.exception.ConflictException;
import com.example.workspace.common.util.Timestamps;
import com.example.workspace.note.domain.Note;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

    private static final Table<Record> NOTES = table("notes");

    private final DSLContext dsl;

    public NoteRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(Note note) {
        dsl.insertInto(NOTES)
                .set(field("id"), note.id())
                .set(field("workspace_id"), note.workspaceId())
                .set(field("project_id"), note.projectId())
                .set(field("title"), note.title())
                .set(field("content"), note.content())
                .set(field("summary"), note.summary())
                .set(field("favorite"), note.favorite())
                .set(field("archived"), note.archived())
                .set(field("created_by"), note.createdBy())
                .set(field("created_at"), Timestamps.toOffset(note.createdAt()))
                .set(field("updated_at"), Timestamps.toOffset(note.updatedAt()))
                .set(field("deleted_at"), Timestamps.toOffset(note.deletedAt()))
                .set(field("version"), note.version())
                .execute();
    }

    public Optional<Note> findById(UUID workspaceId, UUID id) {
        return dsl.selectFrom(NOTES)
                .where(field("id", UUID.class).eq(id))
                .and(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .fetchOptional(this::toNote);
    }

    public List<Note> list(
            UUID workspaceId,
            UUID projectId,
            Boolean favorite,
            boolean archived,
            String keyword,
            PageQuery pageQuery
    ) {
        return dsl.selectFrom(NOTES)
                .where(listCondition(workspaceId, projectId, favorite, archived, keyword))
                .orderBy(resolveSort(pageQuery.sort()))
                .limit(pageQuery.sizeOrDefault())
                .offset(pageQuery.offset())
                .fetch(this::toNote);
    }

    public long count(UUID workspaceId, UUID projectId, Boolean favorite, boolean archived, String keyword) {
        return dsl.fetchCount(NOTES, listCondition(workspaceId, projectId, favorite, archived, keyword));
    }

    public List<Note> listRecent(UUID workspaceId, int limit) {
        return dsl.selectFrom(NOTES)
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .and(field("archived", Boolean.class).eq(false))
                .orderBy(field("updated_at").desc())
                .limit(limit)
                .fetch(this::toNote);
    }

    public long countVisible(UUID workspaceId) {
        return dsl.fetchCount(
                NOTES,
                field("workspace_id", UUID.class).eq(workspaceId)
                        .and(field("deleted_at").isNull())
                        .and(field("archived", Boolean.class).eq(false))
        );
    }

    public long countCreatedBetween(UUID workspaceId, Instant start, Instant end) {
        return dsl.fetchCount(
                NOTES,
                field("workspace_id", UUID.class).eq(workspaceId)
                        .and(field("deleted_at").isNull())
                        .and(field("created_at").ge(Timestamps.toOffset(start)))
                        .and(field("created_at").lt(Timestamps.toOffset(end)))
        );
    }

    public Note update(Note note, Instant now) {
        int updated = dsl.update(NOTES)
                .set(field("project_id"), note.projectId())
                .set(field("title"), note.title())
                .set(field("content"), note.content())
                .set(field("summary"), note.summary())
                .set(field("favorite"), note.favorite())
                .set(field("archived"), note.archived())
                .set(field("updated_at"), Timestamps.toOffset(now))
                .set(field("version"), note.version() + 1)
                .where(field("id", UUID.class).eq(note.id()))
                .and(field("workspace_id", UUID.class).eq(note.workspaceId()))
                .and(field("deleted_at").isNull())
                .and(field("version", Long.class).eq(note.version()))
                .execute();
        if (updated == 0) {
            throw ConflictException.versionMismatch();
        }
        return findById(note.workspaceId(), note.id())
                .orElseThrow(() -> new IllegalStateException("Note not found after update: " + note.id()));
    }

    public void softDelete(UUID workspaceId, UUID id, Instant now) {
        dsl.update(NOTES)
                .set(field("deleted_at"), Timestamps.toOffset(now))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .where(field("id", UUID.class).eq(id))
                .and(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .execute();
    }

    public void softDeleteByProjectId(UUID workspaceId, UUID projectId, Instant now) {
        dsl.update(NOTES)
                .set(field("deleted_at"), Timestamps.toOffset(now))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("project_id", UUID.class).eq(projectId))
                .and(field("deleted_at").isNull())
                .execute();
    }

    private Condition listCondition(
            UUID workspaceId,
            UUID projectId,
            Boolean favorite,
            boolean archived,
            String keyword
    ) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(field("workspace_id", UUID.class).eq(workspaceId));
        conditions.add(field("deleted_at").isNull());
        conditions.add(field("archived", Boolean.class).eq(archived));
        if (projectId != null) {
            conditions.add(field("project_id", UUID.class).eq(projectId));
        }
        if (favorite != null) {
            conditions.add(field("favorite", Boolean.class).eq(favorite));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            conditions.add(field("title", String.class).likeIgnoreCase(pattern)
                    .or(field("content", String.class).likeIgnoreCase(pattern)));
        }
        return DSL.and(conditions);
    }

    private SortField<?> resolveSort(String sort) {
        String raw = sort == null || sort.isBlank() ? "updatedAt,desc" : sort;
        String[] parts = raw.split(",", 2);
        String name = parts[0].trim();
        boolean desc = parts.length < 2 || !"asc".equalsIgnoreCase(parts[1].trim());
        Field<?> column = switch (name) {
            case "createdAt" -> field("created_at");
            case "title" -> field("title");
            default -> field("updated_at");
        };
        return desc ? column.desc() : column.asc();
    }

    private Note toNote(Record record) {
        return new Note(
                record.get("id", UUID.class),
                record.get("workspace_id", UUID.class),
                record.get("project_id", UUID.class),
                record.get("title", String.class),
                record.get("content", String.class),
                record.get("summary", String.class),
                Boolean.TRUE.equals(record.get("favorite", Boolean.class)),
                Boolean.TRUE.equals(record.get("archived", Boolean.class)),
                record.get("created_by", UUID.class),
                Timestamps.toInstant(record.get("created_at")),
                Timestamps.toInstant(record.get("updated_at")),
                Timestamps.toInstant(record.get("deleted_at")),
                record.get("version", Number.class).longValue()
        );
    }
}
