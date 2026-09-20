package com.example.workspace.workspace.dto;

import com.example.workspace.workspace.application.WorkspaceAccess;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "WorkspaceDetailResponse", description = "工作空间详情，含当前用户角色")
public record WorkspaceDetailResponse(
        @Schema(description = "工作空间 ID") UUID id,
        @Schema(description = "名称", example = "我的工作空间") String name,
        @Schema(description = "IANA 时区", example = "UTC") String timezone,
        @Schema(description = "一周起始日：1=周一 … 7=周日", example = "1", minimum = "1", maximum = "7")
        int weekStartsOn,
        @Schema(description = "当前用户在该空间的角色", example = "OWNER", allowableValues = {"OWNER", "MEMBER"})
        String role,
        @Schema(description = "乐观锁版本", example = "0") long version
) {
    public static WorkspaceDetailResponse from(WorkspaceAccess access) {
        return new WorkspaceDetailResponse(
                access.workspace().id(),
                access.workspace().name(),
                access.workspace().timezone(),
                access.workspace().weekStartsOn(),
                access.member().role().name(),
                access.workspace().version()
        );
    }
}
