package com.example.workspace.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
    public static final String REFRESH_COOKIE = "refreshCookie";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Bean
    OpenAPI workspaceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Personal Workspace API")
                        .version("0.1.0")
                        .description("""
                                个人工作空间 API。成功时直接返回资源 JSON，HTTP 状态码表示结果。

                                ## 认证
                                - **Access Token**：登录/注册/刷新后在 JSON 的 `accessToken` 中返回，有效期默认 15 分钟。请求头：`Authorization: Bearer <token>`。
                                - **Refresh Token**：HttpOnly Cookie `refresh_token`（Path=`/api/v1/auth`，SameSite=Lax），JSON 不返回明文。用于 `/api/v1/auth/refresh` 与 `/api/v1/auth/logout`，刷新时轮换。
                                - Workspace 不写入 JWT，按当前用户从数据库读取默认工作空间。
                                - 修改密码会递增 token epoch，并撤销其他 Refresh Token family。

                                ## 约定
                                - 主键为 UUIDv7。时间字段为 ISO-8601（UTC）。
                                - 写操作携带 `version` 做乐观锁，冲突时返回 `409 VERSION_CONFLICT`。
                                - 分页从 `page=1` 开始，默认 `size=20`，最大 `100`。`sort` 形如 `updatedAt,desc`。
                                - 可传入 `X-Request-Id`；未传时服务端生成并在响应头回写。
                                """))
                .servers(List.of(new Server().url("/").description("当前服务")))
                .tags(List.of(
                        new Tag().name("Auth").description("注册、登录、刷新与退出"),
                        new Tag().name("Current User").description("当前登录用户资料与改密"),
                        new Tag().name("Workspaces").description("工作空间列表与设置"),
                        new Tag().name("Projects").description("项目 CRUD、状态、归档"),
                        new Tag().name("Tasks").description("任务 CRUD 与状态"),
                        new Tag().name("Notes").description("笔记 CRUD 与筛选"),
                        new Tag().name("Dashboard").description("工作台聚合数据")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录/注册/刷新返回的 Access Token。请求头：`Authorization: Bearer <token>`。"))
                        .addSecuritySchemes(REFRESH_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("refresh_token")
                                .description("HttpOnly Refresh Token，仅用于刷新与退出。"))
                        .addSchemas("ErrorResponse", errorResponseSchema())
                        .addExamples("ErrorUnauthorized", errorExample(
                                "UNAUTHORIZED",
                                "Authentication required"
                        ))
                        .addExamples("ErrorForbidden", errorExample(
                                "FORBIDDEN",
                                "Access denied"
                        ))
                        .addExamples("ErrorValidation", errorExample(
                                "VALIDATION_ERROR",
                                "Request validation failed"
                        ))
                        .addExamples("ErrorNotFound", errorExample(
                                "RESOURCE_NOT_FOUND",
                                "资源不存在"
                        ))
                        .addExamples("ErrorConflict", errorExample(
                                "VERSION_CONFLICT",
                                "资源已在其他位置更新，请刷新后重试"
                        ))
                        .addExamples("ErrorBusinessRule", errorExample(
                                "BUSINESS_RULE_VIOLATION",
                                "业务规则不满足"
                        ))
                        .addExamples("ErrorInternal", errorExample(
                                "INTERNAL_ERROR",
                                "An unexpected error occurred"
                        )));
    }

    @Bean
    OpenApiCustomizer commonResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(this::enrichOperation));
        };
    }

    private void enrichOperation(Operation operation) {
        operation.addParametersItem(new Parameter()
                .in("header")
                .name(REQUEST_ID_HEADER)
                .required(false)
                .description("可选请求追踪 ID。未传入时由服务端生成，并在响应头 `X-Request-Id` / `X-Trace-Id` 回写。")
                .schema(new StringSchema().example("0199a1c0-0000-7000-8000-000000000001")));

        var responses = operation.getResponses();
        responses.putIfAbsent("400", errorApiResponse("请求校验失败或参数无效", "ErrorValidation"));
        responses.putIfAbsent("401", errorApiResponse("未认证，或 Access / Refresh Token 无效、过期", "ErrorUnauthorized"));
        responses.putIfAbsent("403", errorApiResponse("已认证但无权限", "ErrorForbidden"));
        responses.putIfAbsent("404", errorApiResponse("资源不存在，或当前用户不可见", "ErrorNotFound"));
        responses.putIfAbsent("409", errorApiResponse("邮箱已注册，或乐观锁版本冲突", "ErrorConflict"));
        responses.putIfAbsent("422", errorApiResponse("业务规则不满足", "ErrorBusinessRule"));
        responses.putIfAbsent("500", errorApiResponse("未预期的服务端错误", "ErrorInternal"));
        responses.forEach((code, response) -> {
            if (response.getHeaders() == null || !response.getHeaders().containsKey(REQUEST_ID_HEADER)) {
                response.addHeaderObject(REQUEST_ID_HEADER, new Header()
                        .description("本次请求的追踪 ID")
                        .schema(new StringSchema()));
            }
        });
    }

    private static ApiResponse errorApiResponse(String description, String exampleName) {
        Schema<Object> schema = new Schema<>();
        schema.$ref("#/components/schemas/ErrorResponse");
        MediaType mediaType = new MediaType()
                .schema(schema)
                .addExamples(exampleName, new Example().$ref("#/components/examples/" + exampleName));
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        mediaType
                ));
    }

    @SuppressWarnings("unchecked")
    private static Schema<?> errorResponseSchema() {
        Schema<Object> detailsItem = new Schema<>();
        detailsItem.type("object");
        detailsItem.addProperty("field", new StringSchema().example("email"));
        detailsItem.addProperty("message", new StringSchema().example("must not be blank"));
        detailsItem.setDescription("字段级校验错误");

        Schema<Object> details = new Schema<>();
        details.type("array");
        details.items(detailsItem);

        Schema<Object> schema = new Schema<>();
        schema.type("object");
        schema.description("统一错误响应");
        schema.addProperty("code", new StringSchema()
                .description("错误码")
                .example("AUTH_INVALID_CREDENTIALS")
                ._enum(List.of(
                        "VALIDATION_ERROR",
                        "UNAUTHORIZED",
                        "FORBIDDEN",
                        "RESOURCE_NOT_FOUND",
                        "CONFLICT",
                        "VERSION_CONFLICT",
                        "BUSINESS_RULE_VIOLATION",
                        "AUTH_INVALID_CREDENTIALS",
                        "AUTH_EMAIL_ALREADY_EXISTS",
                        "AUTH_ACCOUNT_DISABLED",
                        "AUTH_TOKEN_EXPIRED",
                        "AUTH_TOKEN_INVALID",
                        "AUTH_REFRESH_TOKEN_INVALID",
                        "RATE_LIMITED",
                        "INTERNAL_ERROR"
                )));
        schema.addProperty("message", new StringSchema().description("可读错误信息").example("邮箱或密码错误"));
        schema.addProperty("details", details);
        schema.addProperty("requestId", new StringSchema().description("请求追踪 ID").example("0199a1c0-0000-7000-8000-000000000001"));
        schema.setRequired(List.of("code", "message", "details", "requestId"));
        return schema;
    }

    private static Example errorExample(String code, String message) {
        return new Example().value(Map.of(
                "code", code,
                "message", message,
                "details", List.of(),
                "requestId", "0199a1c0-0000-7000-8000-000000000001"
        ));
    }
}
