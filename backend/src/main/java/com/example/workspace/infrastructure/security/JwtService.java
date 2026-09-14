package com.example.workspace.infrastructure.security;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.config.AppProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String ISSUER = "ai-personal-workspace";

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    public JwtService(JwtEncoder jwtEncoder, AppProperties appProperties) {
        this.jwtEncoder = jwtEncoder;
        this.appProperties = appProperties;
    }

    public IssuedAccessToken issueAccessToken(CurrentUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(appProperties.security().jwt().accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(user.userId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_TOKEN_TYPE, ACCESS_TOKEN_TYPE)
                .build();

        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        ));
        return new IssuedAccessToken(jwt.getTokenValue(), expiresAt);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {
        public long expiresInSeconds() {
            return Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        }
    }
}
