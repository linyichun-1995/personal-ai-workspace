package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }
}
