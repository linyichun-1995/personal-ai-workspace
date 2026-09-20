package com.example.workspace.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProjectStatsResponse", description = "项目内任务与笔记计数")
public record ProjectStatsResponse(
        @Schema(description = "任务总数") long taskCount,
        @Schema(description = "已完成任务数") long completedTaskCount,
        @Schema(description = "进行中任务数") long inProgressTaskCount,
        @Schema(description = "笔记数") long noteCount
) {
    public static ProjectStatsResponse empty() {
        return new ProjectStatsResponse(0, 0, 0, 0);
    }

    public int progressPercent() {
        if (taskCount <= 0) {
            return 0;
        }
        return (int) Math.round((completedTaskCount * 100.0) / taskCount);
    }
}
