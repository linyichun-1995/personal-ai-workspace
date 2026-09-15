package com.example.workspace.dashboard.dto;

import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        Overview overview,
        List<TaskItem> todayTasks,
        List<TaskItem> nextTasks,
        List<ProjectItem> activeProjects,
        List<NoteItem> recentNotes,
        List<TaskItem> upcomingTasks,
        TaskStatusCounts taskStatusCounts,
        List<ProjectTaskStats> projectTaskStats
) {
    public record Overview(
            long activeProjects,
            long todayTasks,
            long overdueTasks,
            long notes,
            long notesThisWeek,
            long aiConversations,
            int completionRate,
            long reviewTasks,
            long totalTasks,
            long completedTasks
    ) {
    }

    public record TaskItem(
            UUID id,
            String title,
            UUID projectId,
            String projectName,
            TaskStatus status,
            TaskPriority priority,
            Instant dueAt,
            long version
    ) {
    }

    public record ProjectItem(
            UUID id,
            String name,
            String description,
            ProjectStatus status,
            ProjectPriority priority,
            LocalDate dueDate,
            int progress,
            int memberCount,
            long taskCount,
            long completedTaskCount
    ) {
    }

    public record NoteItem(
            UUID id,
            String title,
            String summary,
            Instant updatedAt,
            UUID projectId
    ) {
    }

    public record TaskStatusCounts(
            long todo,
            long inProgress,
            long review,
            long done
    ) {
    }

    public record ProjectTaskStats(
            UUID projectId,
            String name,
            long taskCount,
            long completedTaskCount
    ) {
    }
}
