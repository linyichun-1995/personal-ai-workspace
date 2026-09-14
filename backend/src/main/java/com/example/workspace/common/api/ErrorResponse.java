package com.example.workspace.common.api;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldError> details,
        String requestId
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode code, String message, String requestId) {
        return new ErrorResponse(code.name(), message, List.of(), requestId);
    }

    public static ErrorResponse of(ErrorCode code, String message, List<FieldError> details, String requestId) {
        return new ErrorResponse(code.name(), message, details, requestId);
    }
}
