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
        return new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS, "邮箱或密码错误");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID, "令牌无效");
    }

    public static UnauthorizedException tokenExpired() {
        return new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED, "令牌已过期");
    }

    public static UnauthorizedException refreshTokenInvalid() {
        return new UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID, "刷新令牌无效");
    }

    public static UnauthorizedException accountDisabled() {
        return new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED, "账号已禁用");
    }
}
