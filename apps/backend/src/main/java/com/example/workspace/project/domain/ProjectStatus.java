package com.example.workspace.project.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目状态：规划中、进行中、暂停、已完成")
public enum ProjectStatus {
    PLANNED,
    ACTIVE,
    PAUSED,
    COMPLETED
}
