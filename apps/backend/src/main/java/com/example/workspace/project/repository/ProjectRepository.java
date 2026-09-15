package com.example.workspace.project.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.exception.ConflictException;
import com.example.workspace.common.util.Timestamps;
import com.example.workspace.project.domain.Project;
import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import com.example.workspace.project.dto.ProjectStatsResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
public class ProjectRepository {

    private static final Table<Record> PROJECTS = table("projects");

    private final DSLContext dsl;

    public ProjectRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(Project project) {
        dsl.insertInto(PROJECTS)
                .set(field("id"), project.id())
                .set(field("workspace_id"), project.workspaceId())
                .set(field("name"), project.name())
                .set(field("description"), project.description())
                .set(field("status"), project.status().name())
                .set(field("priority"), project.priority().name())
                .set(field("start_date"), project.startDate())
                .set(field("due_date"), project.dueDate())
                .set(field("archived_at"), Timestamps.toOffset(project.archivedAt()))
                .set(field("created_by"), project.createdBy())
                .set(field("created_at"), Timestamps.toOffset(project.createdAt()))
                .set(field("updated_at"), Timestamps.toOffset(project.updatedAt()))
                .set(field("deleted_at"), Timestamps.toOffset(project.deletedAt()))
                .set(field("version"), project.version())
                .execute();
    }

    public Optional<Project> findById(UUID workspaceId, UUID id) {
        return dsl.selectFrom(PROJECTS)
                .where(field("id", UUID.class).eq(id))
                .and(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .fetchOptional(this::toProject);
    }

    public List<Project> list(
            UUID workspaceId,
            Set<ProjectStatus> statuses,
            ProjectPriority priority,
            boolean archived,
            PageQuery pageQuery
    ) {
        return dsl.selectFrom(PROJECTS)
                .where(listCondition(workspaceId, statuses, priority, archived))
                .orderBy(resolveSort(pageQuery.sort(), "due_date"))
                .limit(pageQuery.sizeOrDefault())
                .offset(pageQuery.offset())
                .fetch(this::toProject);
    }

    public long count(UUID workspaceId, Set<ProjectStatus> statuses, ProjectPriority priority, boolean archived) {
        return dsl.fetchCount(PROJECTS, listCondition(workspaceId, statuses, priority, archived));
    }

    public List<Project> listActive(UUID workspaceId, int limit) {
        return dsl.selectFrom(PROJECTS)
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .and(field("archived_at").isNull())
                .and(field("status", String.class).in(ProjectStatus.ACTIVE.name(), ProjectStatus.PLANNED.name()))
                .orderBy(field("updated_at").desc())
                .limit(limit)
                .fetch(this::toProject);
    }

    public long countActive(UUID workspaceId) {
        return dsl.fetchCount(
                PROJECTS,
                field("workspace_id", UUID.class).eq(workspaceId)
                        .and(field("deleted_at").isNull())
                        .and(field("archived_at").isNull())
                        .and(field("status", String.class).in(ProjectStatus.ACTIVE.name(), ProjectStatus.PLANNED.name()))
        );
    }

    public Map<UUID, ProjectStatsResponse> statsByProjectIds(UUID workspaceId, List<UUID> projectIds) {
        Map<UUID, ProjectStatsResponse> stats = new HashMap<>();
        if (projectIds.isEmpty()) {
            return stats;
        }
        for (UUID projectId : projectIds) {
            stats.put(projectId, ProjectStatsResponse.empty());
        }
        dsl.select(
                        field("project_id", UUID.class),
                        DSL.count().filterWhere(field("deleted_at").isNull()
                                .and(field("status", String.class).ne("CANCELLED"))),
                        DSL.count().filterWhere(field("deleted_at").isNull()
                                .and(field("status", String.class).eq("DONE"))),
                        DSL.count().filterWhere(field("deleted_at").isNull()
                                .and(field("status", String.class).eq("IN_PROGRESS")))
                )
                .from(table("tasks"))
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("project_id", UUID.class).in(projectIds))
                .groupBy(field("project_id"))
                .fetch()
                .forEach(record -> {
                    UUID projectId = record.get(0, UUID.class);
                    long total = record.get(1, Number.class).longValue();
                    long done = record.get(2, Number.class).longValue();
                    long inProgress = record.get(3, Number.class).longValue();
                    stats.compute(projectId, (id, current) -> {
                        ProjectStatsResponse existing = current == null ? ProjectStatsResponse.empty() : current;
                        return new ProjectStatsResponse(total, done, inProgress, existing.noteCount());
                    });
                });
        dsl.select(
                        field("project_id", UUID.class),
                        DSL.count().filterWhere(field("deleted_at").isNull())
                )
                .from(table("notes"))
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("project_id", UUID.class).in(projectIds))
                .groupBy(field("project_id"))
                .fetch()
                .forEach(record -> {
                    UUID projectId = record.get(0, UUID.class);
                    long notes = record.get(1, Number.class).longValue();
                    stats.compute(projectId, (id, current) -> {
                        ProjectStatsResponse existing = current == null ? ProjectStatsResponse.empty() : current;
                        return new ProjectStatsResponse(
                                existing.taskCount(),
                                existing.completedTaskCount(),
                                existing.inProgressTaskCount(),
                                notes
                        );
                    });
                });
        return stats;
    }

    public Project update(Project project, Instant now) {
        int updated = dsl.update(PROJECTS)
                .set(field("name"), project.name())
                .set(field("description"), project.description())
                .set(field("status"), project.status().name())
                .set(field("priority"), project.priority().name())
                .set(field("start_date"), project.startDate())
                .set(field("due_date"), project.dueDate())
                .set(field("archived_at"), Timestamps.toOffset(project.archivedAt()))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .set(field("version"), project.version() + 1)
                .where(field("id", UUID.class).eq(project.id()))
                .and(field("workspace_id", UUID.class).eq(project.workspaceId()))
                .and(field("deleted_at").isNull())
                .and(field("version", Long.class).eq(project.version()))
                .execute();
        if (updated == 0) {
            throw ConflictException.versionMismatch();
        }
        return findById(project.workspaceId(), project.id())
                .orElseThrow(() -> new IllegalStateException("Project not found after update: " + project.id()));
    }

    public void softDelete(UUID workspaceId, UUID id, Instant now) {
        dsl.update(PROJECTS)
                .set(field("deleted_at"), Timestamps.toOffset(now))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .where(field("id", UUID.class).eq(id))
                .and(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .execute();
    }

    private Condition listCondition(
            UUID workspaceId,
            Set<ProjectStatus> statuses,
            ProjectPriority priority,
            boolean archived
    ) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(field("workspace_id", UUID.class).eq(workspaceId));
        conditions.add(field("deleted_at").isNull());
        if (archived) {
            conditions.add(field("archived_at").isNotNull());
        }
        else {
            conditions.add(field("archived_at").isNull());
        }
        if (statuses != null && !statuses.isEmpty()) {
            conditions.add(field("status", String.class).in(statuses.stream().map(Enum::name).toList()));
        }
        if (priority != null) {
            conditions.add(field("priority", String.class).eq(priority.name()));
        }
        return DSL.and(conditions);
    }

    private SortField<?> resolveSort(String sort, String dateColumn) {
        String raw = sort == null || sort.isBlank() ? "updatedAt,desc" : sort;
        String[] parts = raw.split(",", 2);
        String name = parts[0].trim();
        boolean desc = parts.length < 2 || !"asc".equalsIgnoreCase(parts[1].trim());
        Field<?> column = switch (name) {
            case "createdAt" -> field("created_at");
            case "dueDate", "dueAt" -> field(dateColumn);
            case "priority" -> field("priority");
            case "name", "title" -> field("name");
            case "status" -> field("status");
            default -> field("updated_at");
        };
        return desc ? column.desc() : column.asc();
    }

    private Project toProject(Record record) {
        return new Project(
                record.get("id", UUID.class),
                record.get("workspace_id", UUID.class),
                record.get("name", String.class),
                record.get("description", String.class),
                ProjectStatus.valueOf(record.get("status", String.class)),
                ProjectPriority.valueOf(record.get("priority", String.class)),
                record.get("start_date", LocalDate.class),
                record.get("due_date", LocalDate.class),
                Timestamps.toInstant(record.get("archived_at")),
                record.get("created_by", UUID.class),
                Timestamps.toInstant(record.get("created_at")),
                Timestamps.toInstant(record.get("updated_at")),
                Timestamps.toInstant(record.get("deleted_at")),
                record.get("version", Number.class).longValue()
        );
    }
}
