package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode code, String message) {
        super(code, HttpStatus.UNAUTHORIZED, message);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("Authentication required");
    }

    public static UnauthorizedException accountDisabled() {
        return new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED, "Account is disabled");
    }
}
