package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public AppException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
