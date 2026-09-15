package com.example.workspace.infrastructure.security;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.api.ErrorResponse;
import com.example.workspace.common.api.RequestIdFilter;
import com.example.workspace.infrastructure.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AppProperties appProperties;
    private final JsonMapper jsonMapper;

    public LoginRateLimitFilter(AppProperties appProperties, JsonMapper jsonMapper) {
        this.appProperties = appProperties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        AppProperties.Security.LoginRateLimit rateLimit = appProperties.security().loginRateLimit();
        if (rateLimit.limit() <= 0) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!allow(clientIp(request), rateLimit.limit(), rateLimit.window())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            jsonMapper.writeValue(
                    response.getOutputStream(),
                    ErrorResponse.of(ErrorCode.RATE_LIMITED, "请求过于频繁", MDC.get(RequestIdFilter.MDC_REQUEST_ID))
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allow(String clientIp, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1, window.toMillis());
        Window current = windows.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.resetAtMs <= now) {
                return new Window(new AtomicInteger(1), now + windowMs);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return current.count.get() <= limit;
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record Window(AtomicInteger count, long resetAtMs) {
    }
}
