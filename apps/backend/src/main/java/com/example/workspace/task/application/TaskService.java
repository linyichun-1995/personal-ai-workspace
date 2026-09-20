package com.example.workspace.task.application;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.domain.SourceType;
import com.example.workspace.file.application.FileLifecycle;
import com.example.workspace.common.exception.BusinessException;
import com.example.workspace.common.exception.ResourceNotFoundException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.common.util.WorkspaceClock;
import com.example.workspace.project.application.ProjectService;
import com.example.workspace.project.domain.Project;
import com.example.workspace.project.repository.ProjectRepository;
import com.example.workspace.task.domain.Task;
import com.example.workspace.task.domain.TaskDueFilter;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import com.example.workspace.task.dto.CreateTaskRequest;
import com.example.workspace.task.dto.TaskResponse;
import com.example.workspace.task.dto.UpdateTaskRequest;
import com.example.workspace.task.dto.UpdateTaskStatusRequest;
import com.example.workspace.task.repository.TaskRepository;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import com.example.workspace.workspace.application.WorkspaceAccess;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final CurrentWorkspaceResolver currentWorkspaceResolver;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final FileLifecycle fileLifecycle;

    public TaskService(
            CurrentWorkspaceResolver currentWorkspaceResolver,
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectService projectService,
            FileLifecycle fileLifecycle
    ) {
        this.currentWorkspaceResolver = currentWorkspaceResolver;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.fileLifecycle = fileLifecycle;
    }

    @Transactional
    public TaskResponse create(CurrentUser currentUser, CreateTaskRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Instant now = Instant.now();
        UUID workspaceId = access.workspace().id();
        Task parent = resolveParent(workspaceId, request.parentId());
        UUID projectId = resolveProjectId(workspaceId, request.projectId(), parent);
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        Instant completedAt = status == TaskStatus.DONE ? now : null;
        validateTimes(request.startAt(), request.dueAt());
        Task task = new Task(
                UuidV7.next(),
                workspaceId,
                projectId,
                parent == null ? null : parent.id(),
                request.title().trim(),
                normalizeText(request.description()),
                status,
                request.priority() == null ? TaskPriority.MEDIUM : request.priority(),
                request.startAt(),
                request.dueAt(),
                completedAt,
                currentUser.userId(),
                now,
                now,
                null,
                0
        );
        taskRepository.insert(task);
        return toResponse(workspaceId, task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(
            CurrentUser currentUser,
            UUID projectId,
            String status,
            TaskPriority priority,
            TaskDueFilter due,
            PageQuery pageQuery
    ) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        if (projectId != null) {
            projectService.requireProject(workspaceId, projectId);
        }
        WorkspaceClock.Windows windows = WorkspaceClock.of(access.workspace(), Instant.now());
        Set<TaskStatus> statuses = parseStatuses(status);
        List<Task> tasks = taskRepository.list(workspaceId, projectId, statuses, priority, due, windows, pageQuery);
        long total = taskRepository.count(workspaceId, projectId, statuses, priority, due, windows);
        Map<UUID, Project> projects = projectNames(workspaceId, tasks);
        return PageResponse.of(
                tasks.stream().map(task -> TaskResponse.from(task, projectName(projects, task.projectId()))).toList(),
                pageQuery,
                total
        );
    }

    @Transactional(readOnly = true)
    public TaskResponse get(CurrentUser currentUser, UUID taskId) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Task task = requireTask(access.workspace().id(), taskId);
        return toResponse(access.workspace().id(), task);
    }

    @Transactional
    public TaskResponse update(CurrentUser currentUser, UUID taskId, UpdateTaskRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        Task task = requireTask(workspaceId, taskId);
        Task parent = resolveParent(workspaceId, request.parentId());
        if (parent != null && parent.id().equals(task.id())) {
            throw new BusinessException("任务不能作为自己的父任务");
        }
        UUID projectId = resolveProjectId(workspaceId, request.projectId(), parent);
        validateTimes(request.startAt(), request.dueAt());
        fileLifecycle.checkMove(workspaceId, SourceType.TASK, taskId, task.projectId(), projectId);
        Instant completedAt = completedAtFor(task.completedAt(), task.status(), request.status(), Instant.now());
        Task updated = taskRepository.update(new Task(
                task.id(),
                task.workspaceId(),
                projectId,
                parent == null ? null : parent.id(),
                request.title().trim(),
                normalizeText(request.description()),
                request.status(),
                request.priority(),
                request.startAt(),
                request.dueAt(),
                completedAt,
                task.createdBy(),
                task.createdAt(),
                task.updatedAt(),
                task.deletedAt(),
                request.version()
        ), Instant.now());
        return toResponse(workspaceId, updated);
    }

    @Transactional
    public TaskResponse updateStatus(CurrentUser currentUser, UUID taskId, UpdateTaskStatusRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        Task task = requireTask(workspaceId, taskId);
        Instant now = Instant.now();
        Instant completedAt = completedAtFor(task.completedAt(), task.status(), request.status(), now);
        Task updated = taskRepository.update(new Task(
                task.id(),
                task.workspaceId(),
                task.projectId(),
                task.parentId(),
                task.title(),
                task.description(),
                request.status(),
                task.priority(),
                task.startAt(),
                task.dueAt(),
                completedAt,
                task.createdBy(),
                task.createdAt(),
                task.updatedAt(),
                task.deletedAt(),
                request.version()
        ), now);
        return toResponse(workspaceId, updated);
    }

    @Transactional
    public void delete(CurrentUser currentUser, UUID taskId) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Task task = requireTask(access.workspace().id(), taskId);
        Instant now = Instant.now();
        taskRepository.softDeleteChildren(task.workspaceId(), task.id(), now);
        taskRepository.softDelete(task.workspaceId(), task.id(), now);
    }

    private Task requireTask(UUID workspaceId, UUID taskId) {
        Task task = taskRepository.findById(workspaceId, taskId).orElseThrow(ResourceNotFoundException::task);
        fileLifecycle.checkParentWrite(workspaceId, task.projectId());
        return task;
    }

    private Task resolveParent(UUID workspaceId, UUID parentId) {
        if (parentId == null) {
            return null;
        }
        Task parent = taskRepository.findById(workspaceId, parentId).orElseThrow(ResourceNotFoundException::task);
        if (parent.parentId() != null) {
            throw new BusinessException("V0.1 仅支持一级子任务");
        }
        return parent;
    }

    private UUID resolveProjectId(UUID workspaceId, UUID requestedProjectId, Task parent) {
        UUID projectId = requestedProjectId;
        if (parent != null) {
            if (projectId != null && parent.projectId() != null && !parent.projectId().equals(projectId)) {
                throw new BusinessException("子任务必须属于与父任务相同的项目");
            }
            if (projectId == null) {
                projectId = parent.projectId();
            }
        }
        if (projectId != null) {
            Project project = projectService.requireProject(workspaceId, projectId);
            projectService.requireAssignable(project);
        }
        return projectId;
    }

    private TaskResponse toResponse(UUID workspaceId, Task task) {
        String projectName = null;
        if (task.projectId() != null) {
            projectName = projectRepository.findById(workspaceId, task.projectId()).map(Project::name).orElse(null);
        }
        return TaskResponse.from(task, projectName);
    }

    private Map<UUID, Project> projectNames(UUID workspaceId, List<Task> tasks) {
        return tasks.stream()
                .map(Task::projectId)
                .filter(id -> id != null)
                .distinct()
                .map(id -> projectRepository.findById(workspaceId, id).orElse(null))
                .filter(project -> project != null)
                .collect(Collectors.toMap(Project::id, Function.identity()));
    }

    private static String projectName(Map<UUID, Project> projects, UUID projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projects.get(projectId);
        return project == null ? null : project.name();
    }

    private static Instant completedAtFor(Instant current, TaskStatus from, TaskStatus to, Instant now) {
        if (to == TaskStatus.DONE) {
            return from == TaskStatus.DONE && current != null ? current : now;
        }
        return null;
    }

    private static void validateTimes(Instant startAt, Instant dueAt) {
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt)) {
            throw new BusinessException("截止时间不能早于开始时间");
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Set<TaskStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return Set.of();
        }
        try {
            return Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(TaskStatus::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(TaskStatus.class)));
        }
        catch (IllegalArgumentException exception) {
            throw new BusinessException("任务状态无效");
        }
    }
}
