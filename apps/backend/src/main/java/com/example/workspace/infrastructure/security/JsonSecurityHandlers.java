package com.example.workspace.infrastructure.security;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.api.ErrorResponse;
import com.example.workspace.common.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public JsonSecurityHandlers(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        if (isExpired(authException)) {
            write(response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED, "令牌已过期");
            return;
        }
        if (hasBearerToken(request)) {
            write(response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_INVALID, "令牌无效");
            return;
        }
        write(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied");
    }

    private void write(HttpServletResponse response, HttpStatus status, ErrorCode code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(
                response.getOutputStream(),
                ErrorResponse.of(code, message, MDC.get(RequestIdFilter.MDC_REQUEST_ID))
        );
    }

    private static boolean hasBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    private static boolean isExpired(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof JwtValidationException jwtValidationException) {
                boolean expiredError = jwtValidationException.getErrors().stream().anyMatch(error -> {
                    String description = error.getDescription();
                    return description != null && description.toLowerCase().contains("expired");
                });
                if (expiredError) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("expired")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
