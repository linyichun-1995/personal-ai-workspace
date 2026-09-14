package com.example.workspace.infrastructure.config;

import com.example.workspace.infrastructure.security.JsonSecurityHandlers;
import com.example.workspace.infrastructure.security.JwtToCurrentUserConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * CSRF is disabled because access tokens are sent as Bearer tokens in the
     * Authorization header. Browser cookie-based CSRF does not apply. If refresh
     * tokens later move to HttpOnly cookies, enable CSRF for those endpoints.
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtToCurrentUserConverter jwtToCurrentUserConverter,
            JsonSecurityHandlers jsonSecurityHandlers
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtToCurrentUserConverter))
                        .authenticationEntryPoint(jsonSecurityHandlers)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonSecurityHandlers)
                        .accessDeniedHandler(jsonSecurityHandlers)
                );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
