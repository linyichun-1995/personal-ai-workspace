# AI Personal Workspace Backend

模块化单体的 Spring Boot 基础工程。当前只包含认证、当前用户和默认 Workspace，不包含 Project / Task / Note / AI 业务。

## 本地启动

```bash
docker compose up -d
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

默认地址：`http://localhost:8080`

## API

| Method | Path | 访问 |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/auth/logout` | Protected |
| GET | `/api/v1/users/me` | Protected |
| GET | `/actuator/health` | Public |

成功响应直接返回资源 JSON，HTTP 状态码表示结果。失败响应：

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "details": [],
  "requestId": "..."
}
```

请求头 `X-Request-Id` 会回写；未传入时服务端生成。

## 认证

- Access Token：HS256 JWT，默认 15 分钟。claims：`sub=userId`、`workspaceId`、`email`、`tokenType=access`
- Refresh Token：不透明随机串，SHA-256 后存 Redis，默认 14 天，刷新时轮换
- 密码：BCrypt，不写明文
- 业务代码通过 `CurrentUser` / `CurrentUserProvider` 取 `userId`、`workspaceId`，不要自己解析 JWT

CSRF：当前 Access Token 走 `Authorization: Bearer`，不使用 Cookie 会话，因此关闭 CSRF。如果以后把 Refresh Token 改成 HttpOnly Cookie，需要为对应接口重新打开 CSRF。

## jOOQ

1. 配置在 `build.gradle.kts` 的 `jooq-codegen-gradle` 插件
2. 生成目录：`backend/build/generated-sources/jooq`
3. 生成代码不提交 Git，它由 Flyway SQL 派生
4. 本地在改完 `src/main/resources/db/migration` 后执行 `./gradlew generateJooq`

当前 Repository 用 jOOQ `DSLContext` 手写字段访问，避免 Controller 直接碰数据库。后续表增多后，把 Repository 切到生成的 `Tables.*`。

## ID

主键使用 UUIDv7：时间有序、适合索引、方便以后同步和外部 Integration。实现见 `UuidV7`。

## 时间

数据库 `TIMESTAMPTZ` 存 UTC。Java 用 `Instant`。API 用 ISO-8601。
