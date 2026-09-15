package com.example.workspace.project.application;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.exception.BusinessException;
import com.example.workspace.common.exception.ResourceNotFoundException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.note.repository.NoteRepository;
import com.example.workspace.project.domain.Project;
import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import com.example.workspace.project.dto.ArchiveProjectRequest;
import com.example.workspace.project.dto.CreateProjectRequest;
import com.example.workspace.project.dto.ProjectResponse;
import com.example.workspace.project.dto.ProjectStatsResponse;
import com.example.workspace.project.dto.UpdateProjectRequest;
import com.example.workspace.project.dto.UpdateProjectStatusRequest;
import com.example.workspace.project.repository.ProjectRepository;
import com.example.workspace.task.repository.TaskRepository;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import com.example.workspace.workspace.application.WorkspaceAccess;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final CurrentWorkspaceResolver currentWorkspaceResolver;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;

    public ProjectService(
            CurrentWorkspaceResolver currentWorkspaceResolver,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            NoteRepository noteRepository
    ) {
        this.currentWorkspaceResolver = currentWorkspaceResolver;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.noteRepository = noteRepository;
    }

    @Transactional
    public ProjectResponse create(CurrentUser currentUser, CreateProjectRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Instant now = Instant.now();
        String name = request.name().trim();
        LocalDate startDate = request.startDate();
        LocalDate dueDate = request.dueDate();
        validateDates(startDate, dueDate);
        Project project = new Project(
                UuidV7.next(),
                access.workspace().id(),
                name,
                normalizeText(request.description()),
                request.status() == null ? ProjectStatus.ACTIVE : request.status(),
                request.priority() == null ? ProjectPriority.MEDIUM : request.priority(),
                startDate,
                dueDate,
                null,
                currentUser.userId(),
                now,
                now,
                null,
                0
        );
        projectRepository.insert(project);
        return ProjectResponse.from(project, ProjectStatsResponse.empty());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(
            CurrentUser currentUser,
            String status,
            ProjectPriority priority,
            Boolean archived,
            PageQuery pageQuery
    ) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        Set<ProjectStatus> statuses = parseStatuses(status);
        boolean archivedOnly = Boolean.TRUE.equals(archived);
        List<Project> projects = projectRepository.list(workspaceId, statuses, priority, archivedOnly, pageQuery);
        long total = projectRepository.count(workspaceId, statuses, priority, archivedOnly);
        Map<UUID, ProjectStatsResponse> stats = projectRepository.statsByProjectIds(
                workspaceId,
                projects.stream().map(Project::id).toList()
        );
        return PageResponse.of(
                projects.stream()
                        .map(project -> ProjectResponse.from(
                                project,
                                stats.getOrDefault(project.id(), ProjectStatsResponse.empty())
                        ))
                        .toList(),
                pageQuery,
                total
        );
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(CurrentUser currentUser, UUID projectId) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Project project = requireProject(access.workspace().id(), projectId);
        Map<UUID, ProjectStatsResponse> stats = projectRepository.statsByProjectIds(
                access.workspace().id(),
                List.of(project.id())
        );
        return ProjectResponse.from(project, stats.getOrDefault(project.id(), ProjectStatsResponse.empty()));
    }

    @Transactional
    public ProjectResponse update(CurrentUser currentUser, UUID projectId, UpdateProjectRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Project project = requireWritable(requireProject(access.workspace().id(), projectId));
        validateDates(request.startDate(), request.dueDate());
        Project updated = projectRepository.update(new Project(
                project.id(),
                project.workspaceId(),
                request.name().trim(),
                normalizeText(request.description()),
                request.status(),
                request.priority(),
                request.startDate(),
                request.dueDate(),
                project.archivedAt(),
                project.createdBy(),
                project.createdAt(),
                project.updatedAt(),
                project.deletedAt(),
                request.version()
        ), Instant.now());
        return withStats(updated);
    }

    @Transactional
    public ProjectResponse updateStatus(CurrentUser currentUser, UUID projectId, UpdateProjectStatusRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Project project = requireWritable(requireProject(access.workspace().id(), projectId));
        Project updated = projectRepository.update(new Project(
                project.id(),
                project.workspaceId(),
                project.name(),
                project.description(),
                request.status(),
                project.priority(),
                project.startDate(),
                project.dueDate(),
                project.archivedAt(),
                project.createdBy(),
                project.createdAt(),
                project.updatedAt(),
                project.deletedAt(),
                request.version()
        ), Instant.now());
        return withStats(updated);
    }

    @Transactional
    public ProjectResponse archive(CurrentUser currentUser, UUID projectId, ArchiveProjectRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Project project = requireProject(access.workspace().id(), projectId);
        if (project.archived()) {
            return withStats(project);
        }
        Instant now = Instant.now();
        Project updated = projectRepository.update(new Project(
                project.id(),
                project.workspaceId(),
                project.name(),
                project.description(),
                project.status(),
                project.priority(),
                project.startDate(),
                project.dueDate(),
                now,
                project.createdBy(),
                project.createdAt(),
                project.updatedAt(),
                project.deletedAt(),
                request.version()
        ), now);
        return withStats(updated);
    }

    @Transactional
    public ProjectResponse restore(CurrentUser currentUser, UUID projectId, ArchiveProjectRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Project project = requireProject(access.workspace().id(), projectId);
        if (!project.archived()) {
            return withStats(project);
        }
        Project updated = projectRepository.update(new Project(
                project.id(),
                project.workspaceId(),
                project.name(),
                project.description(),
                project.status(),
                project.priority(),
                project.startDate(),
                project.dueDate(),
                null,
                project.createdBy(),
                project.createdAt(),
                project.updatedAt(),
                project.deletedAt(),
                request.version()
        ), Instant.now());
        return withStats(updated);
    }

    @Transactional
    public void delete(CurrentUser currentUser, UUID projectId) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Project project = requireProject(access.workspace().id(), projectId);
        Instant now = Instant.now();
        taskRepository.softDeleteByProjectId(project.workspaceId(), project.id(), now);
        noteRepository.softDeleteByProjectId(project.workspaceId(), project.id(), now);
        projectRepository.softDelete(project.workspaceId(), project.id(), now);
    }

    public Project requireProject(UUID workspaceId, UUID projectId) {
        return projectRepository.findById(workspaceId, projectId)
                .orElseThrow(ResourceNotFoundException::project);
    }

    public void requireAssignable(Project project) {
        if (project.archived()) {
            throw new BusinessException("已归档项目不能新增任务或笔记");
        }
    }

    private Project requireWritable(Project project) {
        if (project.archived()) {
            throw new BusinessException("已归档项目为只读，请先恢复后再编辑");
        }
        return project;
    }

    private ProjectResponse withStats(Project project) {
        Map<UUID, ProjectStatsResponse> stats = projectRepository.statsByProjectIds(
                project.workspaceId(),
                List.of(project.id())
        );
        return ProjectResponse.from(project, stats.getOrDefault(project.id(), ProjectStatsResponse.empty()));
    }

    private static void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new BusinessException("截止日期不能早于开始日期");
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Set<ProjectStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return Set.of();
        }
        try {
            return Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(ProjectStatus::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ProjectStatus.class)));
        }
        catch (IllegalArgumentException exception) {
            throw new BusinessException("项目状态无效");
        }
    }
}
