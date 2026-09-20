package com.example.workspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "UserResponse", description = "用户摘要")
public record UserResponse(
        @Schema(description = "用户 ID") UUID id,
        @Schema(description = "邮箱", example = "ada@example.com") String email,
        @Schema(description = "显示名", example = "Ada") String name,
        @Schema(description = "头像 URL，未设置时为 null") String avatarUrl
) {
}
