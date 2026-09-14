package com.example.workspace.infrastructure.security;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.redis.TokenEpochStore;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtToCurrentUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final TokenEpochStore tokenEpochStore;

    public JwtToCurrentUserConverter(TokenEpochStore tokenEpochStore) {
        this.tokenEpochStore = tokenEpochStore;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        long tokenEpoch = readEpoch(jwt);
        if (tokenEpochStore.current(userId) != tokenEpoch) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Token version mismatch", null));
        }
        CurrentUser currentUser = JwtConfig.toCurrentUser(jwt);
        return JwtConfig.authentication(currentUser);
    }

    private static long readEpoch(Jwt jwt) {
        Object claim = jwt.getClaim(JwtService.CLAIM_TOKEN_EPOCH);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
