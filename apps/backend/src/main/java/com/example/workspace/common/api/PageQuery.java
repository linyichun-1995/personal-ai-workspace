package com.example.workspace.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(name = "PageQuery", description = "分页与排序查询参数")
public record PageQuery(
        @Schema(description = "页码，从 1 开始", defaultValue = "1", example = "1", minimum = "1")
        @Min(1) Integer page,
        @Schema(description = "每页条数，最大 100", defaultValue = "20", example = "20", minimum = "1", maximum = "100")
        @Min(1) @Max(MAX_SIZE) Integer size,
        @Schema(description = "排序，格式 `字段,方向`。常用字段：`updatedAt`、`createdAt`、`dueDate`/`dueAt`、`priority`、`name`/`title`、`status`", example = "updatedAt,desc")
        String sort
) {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public int pageOrDefault() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int sizeOrDefault() {
        int resolved = size == null ? DEFAULT_SIZE : size;
        return Math.min(resolved, MAX_SIZE);
    }

    public int offset() {
        return (pageOrDefault() - 1) * sizeOrDefault();
    }
}
