package com.example.workspace.dashboard.dto;

import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.domain.ProjectStatus;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "DashboardResponse", description = "工作台聚合数据")
public record DashboardResponse(
        @Schema(description = "概览计数") Overview overview,
        @Schema(description = "今日到期任务") List<TaskItem> todayTasks,
        @Schema(description = "下一步任务") List<TaskItem> nextTasks,
        @Schema(description = "活跃项目") List<ProjectItem> activeProjects,
        @Schema(description = "最近笔记") List<NoteItem> recentNotes,
        @Schema(description = "即将到期任务") List<TaskItem> upcomingTasks,
        @Schema(description = "任务状态分布") TaskStatusCounts taskStatusCounts,
        @Schema(description = "各项目任务完成情况") List<ProjectTaskStats> projectTaskStats
) {
    @Schema(name = "DashboardOverview", description = "工作台概览")
    public record Overview(
            @Schema(description = "活跃项目数") long activeProjects,
            @Schema(description = "今日到期任务数") long todayTasks,
            @Schema(description = "已逾期任务数") long overdueTasks,
            @Schema(description = "笔记总数") long notes,
            @Schema(description = "本周新增笔记数") long notesThisWeek,
            @Schema(description = "AI 会话数，V0.1 固定为 0") long aiConversations,
            @Schema(description = "任务完成率，0-100") int completionRate,
            @Schema(description = "待回顾任务数") long reviewTasks,
            @Schema(description = "任务总数") long totalTasks,
            @Schema(description = "已完成任务数") long completedTasks
    ) {
    }

    @Schema(name = "DashboardTaskItem", description = "工作台任务条目")
    public record TaskItem(
            @Schema(description = "任务 ID") UUID id,
            @Schema(description = "标题") String title,
            @Schema(description = "所属项目 ID") UUID projectId,
            @Schema(description = "所属项目名称") String projectName,
            @Schema(description = "状态") TaskStatus status,
            @Schema(description = "优先级") TaskPriority priority,
            @Schema(description = "截止时间") Instant dueAt,
            @Schema(description = "乐观锁版本") long version
    ) {
    }

    @Schema(name = "DashboardProjectItem", description = "工作台项目条目")
    public record ProjectItem(
            @Schema(description = "项目 ID") UUID id,
            @Schema(description = "名称") String name,
            @Schema(description = "描述") String description,
            @Schema(description = "状态") ProjectStatus status,
            @Schema(description = "优先级") ProjectPriority priority,
            @Schema(description = "截止日期") LocalDate dueDate,
            @Schema(description = "完成进度 0-100") int progress,
            @Schema(description = "成员数") int memberCount,
            @Schema(description = "任务总数") long taskCount,
            @Schema(description = "已完成任务数") long completedTaskCount
    ) {
    }

    @Schema(name = "DashboardNoteItem", description = "工作台笔记条目")
    public record NoteItem(
            @Schema(description = "笔记 ID") UUID id,
            @Schema(description = "标题") String title,
            @Schema(description = "摘要") String summary,
            @Schema(description = "更新时间") Instant updatedAt,
            @Schema(description = "关联项目 ID") UUID projectId
    ) {
    }

    @Schema(name = "DashboardTaskStatusCounts", description = "任务状态计数")
    public record TaskStatusCounts(
            @Schema(description = "待办") long todo,
            @Schema(description = "进行中") long inProgress,
            @Schema(description = "待回顾") long review,
            @Schema(description = "已完成") long done
    ) {
    }

    @Schema(name = "DashboardProjectTaskStats", description = "项目任务统计")
    public record ProjectTaskStats(
            @Schema(description = "项目 ID") UUID projectId,
            @Schema(description = "项目名称") String name,
            @Schema(description = "任务总数") long taskCount,
            @Schema(description = "已完成任务数") long completedTaskCount
    ) {
    }
}
