package com.example.workspace.workspace.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.workspace.workspace.domain.WorkspaceMember;
import com.example.workspace.workspace.domain.WorkspaceRole;
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
public class WorkspaceMemberRepository {

    private static final Table<Record> WORKSPACE_MEMBERS = table("workspace_members");

    private final DSLContext dsl;

    public WorkspaceMemberRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(WorkspaceMember member) {
        dsl.insertInto(WORKSPACE_MEMBERS)
                .set(field("id"), member.id())
                .set(field("workspace_id"), member.workspaceId())
                .set(field("user_id"), member.userId())
                .set(field("role"), member.role().name())
                .set(field("joined_at"), member.joinedAt().atOffset(ZoneOffset.UTC))
                .execute();
    }

    public Optional<WorkspaceMember> findByUserId(UUID userId) {
        return dsl.selectFrom(WORKSPACE_MEMBERS)
                .where(field("user_id", UUID.class).eq(userId))
                .orderBy(field("joined_at").asc())
                .limit(1)
                .fetchOptional(this::toMember);
    }

    private WorkspaceMember toMember(Record record) {
        return new WorkspaceMember(
                record.get("id", UUID.class),
                record.get("workspace_id", UUID.class),
                record.get("user_id", UUID.class),
                WorkspaceRole.valueOf(record.get("role", String.class)),
                toInstant(record.get("joined_at"))
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
