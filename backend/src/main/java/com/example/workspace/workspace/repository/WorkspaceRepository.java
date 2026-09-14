package com.example.workspace.workspace.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.workspace.workspace.domain.Workspace;
import com.example.workspace.workspace.domain.WorkspaceType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceRepository {

    private static final Table<Record> WORKSPACES = table("workspaces");

    private final DSLContext dsl;

    public WorkspaceRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(Workspace workspace) {
        dsl.insertInto(WORKSPACES)
                .set(field("id"), workspace.id())
                .set(field("name"), workspace.name())
                .set(field("slug"), workspace.slug())
                .set(field("type"), workspace.type().name())
                .set(field("timezone"), workspace.timezone())
                .set(field("week_starts_on"), workspace.weekStartsOn())
                .set(field("created_at"), workspace.createdAt().atOffset(ZoneOffset.UTC))
                .set(field("updated_at"), workspace.updatedAt().atOffset(ZoneOffset.UTC))
                .set(field("version"), workspace.version())
                .execute();
    }

    public Optional<Workspace> findById(UUID id) {
        return dsl.selectFrom(WORKSPACES)
                .where(field("id", UUID.class).eq(id))
                .fetchOptional(this::toWorkspace);
    }

    private Workspace toWorkspace(Record record) {
        return new Workspace(
                record.get("id", UUID.class),
                record.get("name", String.class),
                record.get("slug", String.class),
                WorkspaceType.valueOf(record.get("type", String.class)),
                record.get("timezone", String.class),
                record.get("week_starts_on", Number.class).intValue(),
                toInstant(record.get("created_at")),
                toInstant(record.get("updated_at")),
                record.get("version", Number.class).longValue()
        );
    }

    private static Instant toInstant(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
    }
}
