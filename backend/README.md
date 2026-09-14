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
| POST | `/api/v1/auth/logout` | Public（凭 Refresh Cookie 撤销） |
| GET | `/api/v1/me` | Protected |
| GET | `/actuator/health` | Public |

成功响应直接返回资源 JSON，HTTP 状态码表示结果。失败响应：

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "邮箱或密码错误",
  "details": [],
  "requestId": "..."
}
```

请求头 `X-Request-Id` 会回写；未传入时服务端生成。

## 认证

- Access Token：HS256 JWT，默认 15 分钟。claims：`sub=userId`、`email`、`tokenType=access`。只通过 JSON 返回，前端保存在内存。
- Refresh Token：不透明随机串，SHA-256 后存 Redis，默认 30 天，刷新时轮换。通过 HttpOnly Cookie `refresh_token` 下发，JSON 不返回明文。
- 密码：Argon2，不写明文
- 业务代码通过 `CurrentUser` / `CurrentUserProvider` 取 `userId` 和 `email`，不要自己解析 JWT
- Workspace 不写入 JWT，按当前用户从数据库读取默认工作空间

CSRF：Access Token 走 `Authorization: Bearer`。Refresh Cookie 使用 HttpOnly + SameSite=Lax，跨站 POST 不会带上 Cookie。本地开发通过 Vite 把 `/api` 代理到后端，前后端对浏览器是同站。

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
