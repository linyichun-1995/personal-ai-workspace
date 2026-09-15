package com.example.workspace.project.dto;

public record ProjectStatsResponse(
        long taskCount,
        long completedTaskCount,
        long inProgressTaskCount,
        long noteCount
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
