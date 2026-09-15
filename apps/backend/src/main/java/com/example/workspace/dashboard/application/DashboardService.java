package com.example.workspace.dashboard.application;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.WorkspaceClock;
import com.example.workspace.dashboard.dto.DashboardResponse;
import com.example.workspace.dashboard.dto.DashboardResponse.NoteItem;
import com.example.workspace.dashboard.dto.DashboardResponse.Overview;
import com.example.workspace.dashboard.dto.DashboardResponse.ProjectItem;
import com.example.workspace.dashboard.dto.DashboardResponse.ProjectTaskStats;
import com.example.workspace.dashboard.dto.DashboardResponse.TaskItem;
import com.example.workspace.dashboard.dto.DashboardResponse.TaskStatusCounts;
import com.example.workspace.note.repository.NoteRepository;
import com.example.workspace.project.domain.Project;
import com.example.workspace.project.dto.ProjectStatsResponse;
import com.example.workspace.project.repository.ProjectRepository;
import com.example.workspace.task.domain.Task;
import com.example.workspace.task.domain.TaskStatus;
import com.example.workspace.task.repository.TaskRepository;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import com.example.workspace.workspace.application.WorkspaceAccess;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int LIST_LIMIT = 8;

    private final CurrentWorkspaceResolver currentWorkspaceResolver;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;

    public DashboardService(
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

    @Transactional(readOnly = true)
    public DashboardResponse get(CurrentUser currentUser) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        Instant now = Instant.now();
        WorkspaceClock.Windows windows = WorkspaceClock.of(access.workspace(), now);

        List<Project> activeProjects = projectRepository.listActive(workspaceId, LIST_LIMIT);
        Map<UUID, ProjectStatsResponse> projectStats = projectRepository.statsByProjectIds(
                workspaceId,
                activeProjects.stream().map(Project::id).toList()
        );

        List<Task> todayTasks = taskRepository.listToday(workspaceId, windows, LIST_LIMIT);
        List<Task> upcomingTasks = taskRepository.listUpcoming(workspaceId, windows, LIST_LIMIT);
        List<Task> nextTasks = taskRepository.listNext(workspaceId, windows, LIST_LIMIT);

        long totalTasks = taskRepository.countOpen(workspaceId);
        long completedTasks = taskRepository.countByStatus(workspaceId, TaskStatus.DONE);
        long todo = taskRepository.countByStatus(workspaceId, TaskStatus.TODO);
        long inProgress = taskRepository.countByStatus(workspaceId, TaskStatus.IN_PROGRESS);
        int completionRate = totalTasks == 0 ? 0 : (int) Math.round((completedTasks * 100.0) / totalTasks);

        Map<UUID, Project> names = loadProjects(workspaceId, todayTasks, upcomingTasks, nextTasks, activeProjects);
        return new DashboardResponse(
                new Overview(
                        projectRepository.countActive(workspaceId),
                        taskRepository.countToday(workspaceId, windows),
                        taskRepository.countOverdue(workspaceId, windows),
                        noteRepository.countVisible(workspaceId),
                        noteRepository.countCreatedBetween(workspaceId, windows.startOfWeek(), windows.startOfNextWeek()),
                        0,
                        completionRate,
                        0,
                        totalTasks,
                        completedTasks
                ),
                todayTasks.stream().map(task -> toTaskItem(task, names)).toList(),
                nextTasks.stream().map(task -> toTaskItem(task, names)).toList(),
                activeProjects.stream().map(project -> toProjectItem(project, projectStats)).toList(),
                noteRepository.listRecent(workspaceId, LIST_LIMIT).stream()
                        .map(note -> new NoteItem(note.id(), note.title(), note.summary(), note.updatedAt(), note.projectId()))
                        .toList(),
                upcomingTasks.stream().map(task -> toTaskItem(task, names)).toList(),
                new TaskStatusCounts(todo, inProgress, 0, completedTasks),
                activeProjects.stream()
                        .map(project -> {
                            ProjectStatsResponse stats = projectStats.getOrDefault(project.id(), ProjectStatsResponse.empty());
                            return new ProjectTaskStats(
                                    project.id(),
                                    project.name(),
                                    stats.taskCount(),
                                    stats.completedTaskCount()
                            );
                        })
                        .toList()
        );
    }

    private Map<UUID, Project> loadProjects(
            UUID workspaceId,
            List<Task> todayTasks,
            List<Task> upcomingTasks,
            List<Task> nextTasks,
            List<Project> activeProjects
    ) {
        java.util.HashMap<UUID, Project> map = new java.util.HashMap<>();
        for (Project project : activeProjects) {
            map.put(project.id(), project);
        }
        java.util.stream.Stream.of(todayTasks, upcomingTasks, nextTasks)
                .flatMap(List::stream)
                .map(Task::projectId)
                .filter(id -> id != null && !map.containsKey(id))
                .distinct()
                .forEach(id -> projectRepository.findById(workspaceId, id).ifPresent(project -> map.put(project.id(), project)));
        return map;
    }

    private static TaskItem toTaskItem(Task task, Map<UUID, Project> projects) {
        String projectName = null;
        if (task.projectId() != null) {
            Project project = projects.get(task.projectId());
            projectName = project == null ? null : project.name();
        }
        return new TaskItem(
                task.id(),
                task.title(),
                task.projectId(),
                projectName,
                task.status(),
                task.priority(),
                task.dueAt(),
                task.version()
        );
    }

    private static ProjectItem toProjectItem(Project project, Map<UUID, ProjectStatsResponse> statsMap) {
        ProjectStatsResponse stats = statsMap.getOrDefault(project.id(), ProjectStatsResponse.empty());
        return new ProjectItem(
                project.id(),
                project.name(),
                project.description(),
                project.status(),
                project.priority(),
                project.dueDate(),
                stats.progressPercent(),
                1,
                stats.taskCount(),
                stats.completedTaskCount()
        );
    }
}
