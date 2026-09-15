package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    public static ResourceNotFoundException workspace() {
        return new ResourceNotFoundException("Workspace 不存在");
    }

    public static ResourceNotFoundException project() {
        return new ResourceNotFoundException("项目不存在");
    }

    public static ResourceNotFoundException task() {
        return new ResourceNotFoundException("任务不存在");
    }

    public static ResourceNotFoundException note() {
        return new ResourceNotFoundException("笔记不存在");
    }
}
