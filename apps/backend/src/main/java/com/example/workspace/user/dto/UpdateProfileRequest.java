package com.example.workspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateProfileRequest", description = "更新当前用户资料")
public record UpdateProfileRequest(
        @Schema(description = "显示名", example = "Ada Lovelace")
        @NotBlank @Size(max = 100) String displayName,
        @Schema(description = "头像 URL，传 null 或省略策略以服务端校验为准", example = "https://example.com/avatar.png")
        @Size(max = 1000) String avatarUrl,
        @Schema(description = "界面语言", example = "zh-CN")
        @NotBlank @Size(max = 20) String locale,
        @Schema(description = "IANA 时区", example = "Asia/Shanghai")
        @NotBlank @Size(max = 50) String timezone,
        @Schema(description = "当前资料版本", example = "0")
        @NotNull Long version
) {
}
