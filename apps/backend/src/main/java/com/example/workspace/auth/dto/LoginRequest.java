package com.example.workspace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "登录请求")
public record LoginRequest(
        @Schema(description = "登录邮箱", example = "ada@example.com")
        @NotBlank @Email String email,
        @Schema(description = "登录密码", example = "password123")
        @NotBlank @Size(max = 128) String password
) {
}
