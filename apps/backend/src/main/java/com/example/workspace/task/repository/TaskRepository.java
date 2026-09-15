package com.example.workspace.task.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.exception.ConflictException;
import com.example.workspace.common.util.Timestamps;
import com.example.workspace.common.util.WorkspaceClock;
import com.example.workspace.task.domain.Task;
import com.example.workspace.task.domain.TaskDueFilter;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
public class TaskRepository {

    private static final Table<Record> TASKS = table("tasks");

    private final DSLContext dsl;

    public TaskRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(Task task) {
        dsl.insertInto(TASKS)
                .set(field("id"), task.id())
                .set(field("workspace_id"), task.workspaceId())
                .set(field("project_id"), task.projectId())
                .set(field("parent_id"), task.parentId())
                .set(field("title"), task.title())
                .set(field("description"), task.description())
                .set(field("status"), task.status().name())
                .set(field("priority"), task.priority().name())
                .set(field("start_at"), Timestamps.toOffset(task.startAt()))
                .set(field("due_at"), Timestamps.toOffset(task.dueAt()))
                .set(field("completed_at"), Timestamps.toOffset(task.completedAt()))
                .set(field("created_by"), task.createdBy())
                .set(field("created_at"), Timestamps.toOffset(task.createdAt()))
                .set(field("updated_at"), Timestamps.toOffset(task.updatedAt()))
                .set(field("deleted_at"), Timestamps.toOffset(task.deletedAt()))
                .set(field("version"), task.version())
                .execute();
    }

    public Optional<Task> findById(UUID workspaceId, UUID id) {
        return dsl.selectFrom(TASKS)
                .where(field("id", UUID.class).eq(id))
                .and(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .fetchOptional(this::toTask);
    }

    public List<Task> list(
            UUID workspaceId,
            UUID projectId,
            Set<TaskStatus> statuses,
            TaskPriority priority,
            TaskDueFilter due,
            WorkspaceClock.Windows windows,
            PageQuery pageQuery
    ) {
        return dsl.selectFrom(TASKS)
                .where(listCondition(workspaceId, projectId, statuses, priority, due, windows))
                .orderBy(resolveSort(pageQuery.sort()))
                .limit(pageQuery.sizeOrDefault())
                .offset(pageQuery.offset())
                .fetch(this::toTask);
    }

    public long count(
            UUID workspaceId,
            UUID projectId,
            Set<TaskStatus> statuses,
            TaskPriority priority,
            TaskDueFilter due,
            WorkspaceClock.Windows windows
    ) {
        return dsl.fetchCount(TASKS, listCondition(workspaceId, projectId, statuses, priority, due, windows));
    }

    public List<Task> listToday(UUID workspaceId, WorkspaceClock.Windows windows, int limit) {
        return dsl.selectFrom(TASKS)
                .where(openDueCondition(workspaceId))
                .and(field("due_at").ge(Timestamps.toOffset(windows.startOfToday())))
                .and(field("due_at").lt(Timestamps.toOffset(windows.startOfTomorrow())))
                .orderBy(field("due_at").asc(), field("priority").desc())
                .limit(limit)
                .fetch(this::toTask);
    }

    public List<Task> listUpcoming(UUID workspaceId, WorkspaceClock.Windows windows, int limit) {
        return dsl.selectFrom(TASKS)
                .where(openDueCondition(workspaceId))
                .and(field("due_at").ge(Timestamps.toOffset(windows.startOfTomorrow())))
                .and(field("due_at").lt(Timestamps.toOffset(windows.startOfUpcomingEnd())))
                .orderBy(field("due_at").asc())
                .limit(limit)
                .fetch(this::toTask);
    }

    public List<Task> listNext(UUID workspaceId, WorkspaceClock.Windows windows, int limit) {
        return dsl.selectFrom(TASKS)
                .where(openDueCondition(workspaceId))
                .and(field("due_at").isNull()
                        .or(field("due_at").ge(Timestamps.toOffset(windows.startOfTomorrow()))))
                .orderBy(field("due_at").asc().nullsLast(), field("created_at").asc())
                .limit(limit)
                .fetch(this::toTask);
    }

    public long countToday(UUID workspaceId, WorkspaceClock.Windows windows) {
        return dsl.fetchCount(
                TASKS,
                openDueCondition(workspaceId)
                        .and(field("due_at").ge(Timestamps.toOffset(windows.startOfToday())))
                        .and(field("due_at").lt(Timestamps.toOffset(windows.startOfTomorrow())))
        );
    }

    public long countOverdue(UUID workspaceId, WorkspaceClock.Windows windows) {
        return dsl.fetchCount(
                TASKS,
                openDueCondition(workspaceId)
                        .and(field("due_at").isNotNull())
                        .and(field("due_at").lt(Timestamps.toOffset(windows.startOfToday())))
        );
    }

    public long countByStatus(UUID workspaceId, TaskStatus status) {
        return dsl.fetchCount(
                TASKS,
                field("workspace_id", UUID.class).eq(workspaceId)
                        .and(field("deleted_at").isNull())
                        .and(field("status", String.class).eq(status.name()))
        );
    }

    public long countOpen(UUID workspaceId) {
        return dsl.fetchCount(
                TASKS,
                field("workspace_id", UUID.class).eq(workspaceId)
                        .and(field("deleted_at").isNull())
                        .and(field("status", String.class).ne(TaskStatus.CANCELLED.name()))
        );
    }

    public Task update(Task task, Instant now) {
        int updated = dsl.update(TASKS)
                .set(field("project_id"), task.projectId())
                .set(field("parent_id"), task.parentId())
                .set(field("title"), task.title())
                .set(field("description"), task.description())
                .set(field("status"), task.status().name())
                .set(field("priority"), task.priority().name())
                .set(field("start_at"), Timestamps.toOffset(task.startAt()))
                .set(field("due_at"), Timestamps.toOffset(task.dueAt()))
                .set(field("completed_at"), Timestamps.toOffset(task.completedAt()))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .set(field("version"), task.version() + 1)
                .where(field("id", UUID.class).eq(task.id()))
                .and(field("workspace_id", UUID.class).eq(task.workspaceId()))
                .and(field("deleted_at").isNull())
                .and(field("version", Long.class).eq(task.version()))
                .execute();
        if (updated == 0) {
            throw ConflictException.versionMismatch();
        }
        return findById(task.workspaceId(), task.id())
                .orElseThrow(() -> new IllegalStateException("Task not found after update: " + task.id()));
    }

    public void softDelete(UUID workspaceId, UUID id, Instant now) {
        dsl.update(TASKS)
                .set(field("deleted_at"), Timestamps.toOffset(now))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .where(field("id", UUID.class).eq(id))
                .and(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("deleted_at").isNull())
                .execute();
    }

    public void softDeleteByProjectId(UUID workspaceId, UUID projectId, Instant now) {
        dsl.update(TASKS)
                .set(field("deleted_at"), Timestamps.toOffset(now))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("project_id", UUID.class).eq(projectId))
                .and(field("deleted_at").isNull())
                .execute();
    }

    public void softDeleteChildren(UUID workspaceId, UUID parentId, Instant now) {
        dsl.update(TASKS)
                .set(field("deleted_at"), Timestamps.toOffset(now))
                .set(field("updated_at"), Timestamps.toOffset(now))
                .where(field("workspace_id", UUID.class).eq(workspaceId))
                .and(field("parent_id", UUID.class).eq(parentId))
                .and(field("deleted_at").isNull())
                .execute();
    }

    private Condition openDueCondition(UUID workspaceId) {
        return field("workspace_id", UUID.class).eq(workspaceId)
                .and(field("deleted_at").isNull())
                .and(field("status", String.class).notIn(TaskStatus.DONE.name(), TaskStatus.CANCELLED.name()));
    }

    private Condition listCondition(
            UUID workspaceId,
            UUID projectId,
            Set<TaskStatus> statuses,
            TaskPriority priority,
            TaskDueFilter due,
            WorkspaceClock.Windows windows
    ) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(field("workspace_id", UUID.class).eq(workspaceId));
        conditions.add(field("deleted_at").isNull());
        if (projectId != null) {
            conditions.add(field("project_id", UUID.class).eq(projectId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            conditions.add(field("status", String.class).in(statuses.stream().map(Enum::name).toList()));
        }
        if (priority != null) {
            conditions.add(field("priority", String.class).eq(priority.name()));
        }
        if (due != null) {
            conditions.add(field("status", String.class).notIn(TaskStatus.DONE.name(), TaskStatus.CANCELLED.name()));
            conditions.add(switch (due) {
                case TODAY -> field("due_at").ge(Timestamps.toOffset(windows.startOfToday()))
                        .and(field("due_at").lt(Timestamps.toOffset(windows.startOfTomorrow())));
                case OVERDUE -> field("due_at").isNotNull()
                        .and(field("due_at").lt(Timestamps.toOffset(windows.startOfToday())));
                case UPCOMING -> field("due_at").ge(Timestamps.toOffset(windows.startOfTomorrow()))
                        .and(field("due_at").lt(Timestamps.toOffset(windows.startOfUpcomingEnd())));
            });
        }
        return DSL.and(conditions);
    }

    private SortField<?> resolveSort(String sort) {
        String raw = sort == null || sort.isBlank() ? "dueAt,asc" : sort;
        String[] parts = raw.split(",", 2);
        String name = parts[0].trim();
        boolean desc = parts.length == 2 && "desc".equalsIgnoreCase(parts[1].trim());
        if (parts.length < 2 && !"dueAt".equals(name) && !"dueDate".equals(name)) {
            desc = true;
        }
        Field<?> column = switch (name) {
            case "createdAt" -> field("created_at");
            case "updatedAt" -> field("updated_at");
            case "priority" -> field("priority");
            case "title" -> field("title");
            case "status" -> field("status");
            default -> field("due_at");
        };
        SortField<?> ordered = desc ? column.desc() : column.asc();
        if ("due_at".equals(column.getName()) || "dueAt".equals(name) || "dueDate".equals(name)) {
            return desc ? field("due_at").desc().nullsLast() : field("due_at").asc().nullsLast();
        }
        return ordered;
    }

    private Task toTask(Record record) {
        return new Task(
                record.get("id", UUID.class),
                record.get("workspace_id", UUID.class),
                record.get("project_id", UUID.class),
                record.get("parent_id", UUID.class),
                record.get("title", String.class),
                record.get("description", String.class),
                TaskStatus.valueOf(record.get("status", String.class)),
                TaskPriority.valueOf(record.get("priority", String.class)),
                Timestamps.toInstant(record.get("start_at")),
                Timestamps.toInstant(record.get("due_at")),
                Timestamps.toInstant(record.get("completed_at")),
                record.get("created_by", UUID.class),
                Timestamps.toInstant(record.get("created_at")),
                Timestamps.toInstant(record.get("updated_at")),
                Timestamps.toInstant(record.get("deleted_at")),
                record.get("version", Number.class).longValue()
        );
    }
}
