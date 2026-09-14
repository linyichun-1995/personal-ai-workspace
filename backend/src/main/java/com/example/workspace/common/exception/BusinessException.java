package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
