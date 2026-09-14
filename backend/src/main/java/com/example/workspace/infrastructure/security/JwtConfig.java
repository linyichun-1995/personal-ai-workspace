package com.example.workspace.infrastructure.security;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(AppProperties appProperties) {
        byte[] secret = appProperties.security().jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public static CurrentUser toCurrentUser(org.springframework.security.oauth2.jwt.Jwt jwt) {
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString(JwtService.CLAIM_EMAIL)
        );
    }

    public static UsernamePasswordAuthenticationToken authentication(CurrentUser user) {
        return new UsernamePasswordAuthenticationToken(user, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
