package com.example.workspace.infrastructure.security;

import com.example.workspace.common.security.CurrentUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtToCurrentUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        CurrentUser currentUser = JwtConfig.toCurrentUser(jwt);
        return JwtConfig.authentication(currentUser);
    }
}
