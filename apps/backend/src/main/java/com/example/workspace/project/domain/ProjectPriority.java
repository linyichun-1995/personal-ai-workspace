package com.example.workspace.project.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目优先级")
public enum ProjectPriority {
    LOW,
    MEDIUM,
    HIGH
}
