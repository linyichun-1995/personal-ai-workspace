package com.example.workspace.task.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任务优先级")
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
