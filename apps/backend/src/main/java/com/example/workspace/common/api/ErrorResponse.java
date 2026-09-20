package com.example.workspace.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ErrorResponse", description = "统一错误响应")
public record ErrorResponse(
        @Schema(description = "错误码", example = "AUTH_INVALID_CREDENTIALS") String code,
        @Schema(description = "可读错误信息", example = "邮箱或密码错误") String message,
        @Schema(description = "字段级校验错误，非校验失败时为空数组") List<FieldError> details,
        @Schema(description = "请求追踪 ID", example = "0199a1c0-0000-7000-8000-000000000001") String requestId
) {
    @Schema(name = "FieldError", description = "字段校验错误")
    public record FieldError(
            @Schema(description = "字段名", example = "email") String field,
            @Schema(description = "错误说明", example = "must not be blank") String message
    ) {
    }

    public static ErrorResponse of(ErrorCode code, String message, String requestId) {
        return new ErrorResponse(code.name(), message, List.of(), requestId);
    }

    public static ErrorResponse of(ErrorCode code, String message, List<FieldError> details, String requestId) {
        return new ErrorResponse(code.name(), message, details, requestId);
    }
}
