package com.example.workspace.infrastructure.security;

import com.example.workspace.infrastructure.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieService {

    private final AppProperties appProperties;

    public RefreshTokenCookieService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void write(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshToken, appProperties.security().jwt().refreshTokenTtl()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String name = appProperties.security().refreshCookie().name();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        AppProperties.Security.RefreshCookie properties = appProperties.security().refreshCookie();
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .path(properties.path())
                .sameSite(properties.sameSite())
                .maxAge(maxAge)
                .build();
    }
}
