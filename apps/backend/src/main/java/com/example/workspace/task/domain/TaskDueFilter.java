package com.example.workspace.task.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任务到期筛选：今天、已逾期、即将到期")
public enum TaskDueFilter {
    TODAY,
    OVERDUE,
    UPCOMING
}
