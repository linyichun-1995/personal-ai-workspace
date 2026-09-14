package com.example.workspace.auth.application;

import com.example.workspace.auth.dto.IssuedTokens;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.infrastructure.config.AppProperties;
import com.example.workspace.infrastructure.redis.RefreshSession;
import com.example.workspace.infrastructure.redis.RefreshTokenStore;
import com.example.workspace.infrastructure.security.JwtService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
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

    public IssuedTokens issue(CurrentUser user) {
        return issue(user, UuidV7.next());
    }

    public IssuedTokens rotate(CurrentUser user, UUID familyId, String previousRefreshToken) {
        refreshTokenStore.revoke(previousRefreshToken);
        return issue(user, familyId);
    }

    public void revoke(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    public void revokeFamily(UUID familyId) {
        refreshTokenStore.revokeFamily(familyId);
    }

    private IssuedTokens issue(CurrentUser user, UUID familyId) {
        JwtService.IssuedAccessToken accessToken = jwtService.issueAccessToken(user);
        String refreshToken = newRefreshToken();
        Instant now = Instant.now();
        refreshTokenStore.save(
                refreshToken,
                new RefreshSession(
                        user.userId(),
                        user.email(),
                        familyId,
                        now.plus(appProperties.security().jwt().refreshTokenTtl()),
                        false
                ),
                appProperties.security().jwt().refreshTokenTtl()
        );
        return IssuedTokens.bearer(accessToken.value(), refreshToken, accessToken.expiresInSeconds());
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
