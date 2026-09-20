package com.example.workspace.infrastructure.openapi;

import com.example.workspace.infrastructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public @interface AuthenticatedApi {
}
