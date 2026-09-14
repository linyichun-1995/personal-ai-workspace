package com.example.workspace.common.api;

import com.example.workspace.common.util.UuidV7;
import com.example.workspace.infrastructure.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    private final AppProperties appProperties;

    public RequestIdFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String headerName = appProperties.requestIdHeader();
        String incoming = request.getHeader(headerName);
        String requestId = incoming == null || incoming.isBlank() ? UuidV7.next().toString() : incoming.trim();

        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(headerName, requestId);
        response.setHeader("X-Trace-Id", requestId);

        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        }
        finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            log.info(
                    "{} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
