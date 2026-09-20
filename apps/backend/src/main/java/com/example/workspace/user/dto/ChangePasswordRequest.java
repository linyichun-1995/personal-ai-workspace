package com.example.workspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangePasswordRequest", description = "修改密码")
public record ChangePasswordRequest(
        @Schema(description = "当前密码")
        @NotBlank String currentPassword,
        @Schema(description = "新密码", minLength = 8, maxLength = 128)
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
