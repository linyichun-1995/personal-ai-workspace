package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {

    public ConflictException(ErrorCode code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }

    public static ConflictException emailTaken() {
        return new ConflictException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, "该邮箱已注册");
    }
}
