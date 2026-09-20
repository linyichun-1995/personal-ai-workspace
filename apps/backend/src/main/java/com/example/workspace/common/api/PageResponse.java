package com.example.workspace.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PageResponse", description = "分页响应")
public record PageResponse<T>(
        @Schema(description = "当前页数据") List<T> items,
        @Schema(description = "当前页码", example = "1") int page,
        @Schema(description = "每页条数", example = "20") int size,
        @Schema(description = "总记录数", example = "42") long total,
        @Schema(description = "总页数", example = "3") int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, PageQuery query, long total) {
        int size = query.sizeOrDefault();
        int totalPages = size == 0 ? 0 : (int) Math.ceil(total / (double) size);
        return new PageResponse<>(items, query.pageOrDefault(), size, total, totalPages);
    }
}
