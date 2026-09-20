package com.example.workspace.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateWorkspaceRequest", description = "更新工作空间设置")
public record UpdateWorkspaceRequest(
        @Schema(description = "名称", example = "个人空间")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "IANA 时区", example = "Asia/Shanghai")
        @NotBlank @Size(max = 50) String timezone,
        @Schema(description = "一周起始日：1=周一 … 7=周日", example = "1")
        @Min(1) @Max(7) int weekStartsOn,
        @Schema(description = "当前工作空间版本", example = "0")
        @NotNull Long version
) {
}
