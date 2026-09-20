package com.example.workspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "CurrentUserResponse", description = "当前登录用户资料")
public record CurrentUserResponse(
        @Schema(description = "用户 ID") UUID id,
        @Schema(description = "邮箱", example = "ada@example.com") String email,
        @Schema(description = "显示名", example = "Ada") String name,
        @Schema(description = "头像 URL，未设置时为 null") String avatarUrl,
        @Schema(description = "界面语言", example = "zh-CN") String locale,
        @Schema(description = "IANA 时区", example = "Asia/Shanghai") String timezone,
        @Schema(description = "乐观锁版本", example = "0") long version,
        @Schema(description = "当前默认工作空间") WorkspaceResponse currentWorkspace
) {
}
