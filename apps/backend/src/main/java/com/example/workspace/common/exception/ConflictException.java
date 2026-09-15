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

    public static ConflictException versionMismatch() {
        return new ConflictException(ErrorCode.VERSION_CONFLICT, "资源已在其他位置更新，请刷新后重试");
    }
}
