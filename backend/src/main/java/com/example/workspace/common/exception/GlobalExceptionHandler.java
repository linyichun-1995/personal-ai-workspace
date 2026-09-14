package com.example.workspace.common.exception;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.api.ErrorResponse;
import com.example.workspace.common.api.RequestIdFilter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException exception) {
        if (exception.status().is5xxServerError()) {
            log.error("Business exception: {}", exception.getMessage(), exception);
        }
        else {
            log.warn("Application exception: {} - {}", exception.code(), exception.getMessage());
        }
        return ResponseEntity.status(exception.status()).body(toBody(exception.code(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return ResponseEntity.badRequest().body(
                ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Request validation failed", details, requestId())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        log.warn("Unreadable request body: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(toBody(ErrorCode.VALIDATION_ERROR, "Malformed request body", List.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        log.warn("Authentication failed: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(toBody(ErrorCode.UNAUTHORIZED, "Authentication required", List.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(toBody(ErrorCode.FORBIDDEN, "Access denied", List.of()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("Method not supported: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(toBody(ErrorCode.VALIDATION_ERROR, "HTTP method not supported", List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(toBody(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", List.of()));
    }

    private static ErrorResponse.FieldError toFieldError(FieldError error) {
        return new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
    }

    private ErrorResponse toBody(ErrorCode code, String message, List<ErrorResponse.FieldError> details) {
        return ErrorResponse.of(code, message, details, requestId());
    }

    private String requestId() {
        return MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    }
}
