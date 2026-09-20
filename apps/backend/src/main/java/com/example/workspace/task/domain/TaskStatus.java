package com.example.workspace.task.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任务状态：待办、进行中、已完成、已取消")
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED
}
