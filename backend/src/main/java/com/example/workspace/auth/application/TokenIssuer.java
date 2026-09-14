package com.example.workspace.auth.application;

import com.example.workspace.auth.dto.TokenResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.infrastructure.config.AppProperties;
import com.example.workspace.infrastructure.redis.RefreshSession;
import com.example.workspace.infrastructure.redis.RefreshTokenStore;
import com.example.workspace.infrastructure.security.JwtService;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class TokenIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final AppProperties appProperties;

    public TokenIssuer(
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore,
            AppProperties appProperties
    ) {
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.appProperties = appProperties;
    }

    public TokenResponse issue(CurrentUser user) {
        return issue(user, UuidV7.next().toString());
    }

    public TokenResponse rotate(CurrentUser user, String familyId, String previousRefreshToken) {
        refreshTokenStore.delete(previousRefreshToken);
        return issue(user, familyId);
    }

    public void revoke(String refreshToken) {
        refreshTokenStore.delete(refreshToken);
    }

    private TokenResponse issue(CurrentUser user, String familyId) {
        JwtService.IssuedAccessToken accessToken = jwtService.issueAccessToken(user);
        String refreshToken = newRefreshToken();
        refreshTokenStore.save(
                refreshToken,
                new RefreshSession(user.userId(), user.workspaceId(), user.email(), familyId),
                appProperties.security().jwt().refreshTokenTtl()
        );
        return TokenResponse.bearer(accessToken.value(), refreshToken, accessToken.expiresInSeconds());
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
