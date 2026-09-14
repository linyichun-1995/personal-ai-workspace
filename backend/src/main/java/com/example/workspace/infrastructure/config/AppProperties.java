package com.example.workspace.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String requestIdHeader,
        Cors cors,
        Security security
) {
    public record Cors(
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            boolean allowCredentials
    ) {
    }

    public record Security(
            Jwt jwt,
            LoginRateLimit loginRateLimit,
            RefreshCookie refreshCookie
    ) {
        public record Jwt(
                String secret,
                Duration accessTokenTtl,
                Duration refreshTokenTtl
        ) {
        }

        public record LoginRateLimit(
                int limit,
                Duration window
        ) {
        }

        public record RefreshCookie(
                String name,
                String path,
                boolean secure,
                String sameSite
        ) {
        }
    }
}
