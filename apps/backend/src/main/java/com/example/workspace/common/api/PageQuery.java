package com.example.workspace.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(
        @Min(1) Integer page,
        @Min(1) @Max(MAX_SIZE) Integer size,
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
