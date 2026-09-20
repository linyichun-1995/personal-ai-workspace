package com.example.workspace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "注册请求")
public record RegisterRequest(
        @Schema(description = "登录邮箱，服务端会规范化为小写", example = "ada@example.com")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "登录密码", example = "password123", minLength = 8, maxLength = 128)
        @NotBlank @Size(min = 8, max = 128) String password,
        @Schema(description = "显示名", example = "Ada")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "IANA 时区。省略时默认为 UTC", example = "Asia/Shanghai")
        @Size(max = 50) String timezone
) {
}
