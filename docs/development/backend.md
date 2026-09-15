# 后端开发指南

模块化单体的 Spring Boot 工程。当前包含认证、当前用户、Workspace，以及 Project / Task / Note / Dashboard。

[文档索引](../README.md) · [后端应用](../../apps/backend/README.md) · [目录与扩展约定](../architecture/repository-layout.md)

除非特别说明，下文源码和配置路径均相对于 `apps/backend/`。

## 本地启动

需要 JDK 25。以下命令在仓库根目录开始执行：

```powershell
docker compose up -d
cd apps/backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS / Linux 将 `.\gradlew.bat` 换成 `./gradlew`。

默认地址：`http://localhost:8080`

配置入口为 `src/main/resources/application*.yml`。可参考 [`.env.example`](../../apps/backend/.env.example) 设置进程环境变量；Spring Boot 当前配置不会自动加载应用目录的 `.env` 文件。`local` 配置已提供连接本地 Compose 服务的默认值。

## API

| Method | Path | 访问 |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/auth/logout` | Public（凭 Refresh Cookie 撤销） |
| GET | `/api/v1/me` | Protected |
| PATCH | `/api/v1/me` | Protected，更新资料 |
| PUT | `/api/v1/me/password` | Protected，改密并作废其他会话 |
| GET | `/api/v1/workspaces` | Protected |
| GET | `/api/v1/workspaces/{workspaceId}` | Protected，非成员 404 |
| PATCH | `/api/v1/workspaces/{workspaceId}` | Protected，仅 OWNER |
| POST | `/api/v1/projects` | Protected |
| GET | `/api/v1/projects` | Protected |
| GET | `/api/v1/projects/{id}` | Protected |
| PUT | `/api/v1/projects/{id}` | Protected |
| PATCH | `/api/v1/projects/{id}/status` | Protected |
| POST | `/api/v1/projects/{id}/archive` | Protected |
| POST | `/api/v1/projects/{id}/restore` | Protected |
| DELETE | `/api/v1/projects/{id}` | Protected，软删除并级联任务/笔记 |
| POST | `/api/v1/tasks` | Protected |
| GET | `/api/v1/tasks` | Protected |
| GET | `/api/v1/tasks/{id}` | Protected |
| PUT | `/api/v1/tasks/{id}` | Protected |
| PATCH | `/api/v1/tasks/{id}/status` | Protected |
| DELETE | `/api/v1/tasks/{id}` | Protected |
| POST | `/api/v1/notes` | Protected |
| GET | `/api/v1/notes` | Protected |
| GET | `/api/v1/notes/{id}` | Protected |
| PUT | `/api/v1/notes/{id}` | Protected |
| DELETE | `/api/v1/notes/{id}` | Protected |
| GET | `/api/v1/dashboard` | Protected |
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

- Access Token：HS256 JWT，默认 15 分钟。claims：`sub=userId`、`email`、`tokenType=access`、`ver=tokenEpoch`。只通过 JSON 返回，前端保存在内存。
- Refresh Token：不透明随机串，SHA-256 后存 Redis，默认 30 天，刷新时轮换。通过 HttpOnly Cookie `refresh_token` 下发，JSON 不返回明文。按用户索引，改密时撤销全部 family。
- 密码：Argon2，不写明文。修改密码会递增 token epoch，旧 Access Token 立即失效。
- 业务代码通过 `CurrentUser` / `CurrentUserProvider` 取 `userId` 和 `email`，不要自己解析 JWT
- Workspace 不写入 JWT，按当前用户从数据库读取默认工作空间

CSRF：Access Token 走 `Authorization: Bearer`。Refresh Cookie 使用 HttpOnly + SameSite=Lax，跨站 POST 不会带上 Cookie。本地开发通过 Vite 把 `/api` 代理到后端，前后端对浏览器是同站。

## jOOQ

1. 配置在 `build.gradle.kts` 的 `jooq-codegen-gradle` 插件
2. 生成目录：应用内的 `build/generated-sources/jooq`
3. 生成代码不提交 Git，它由 Flyway SQL 派生
4. 本地在改完 `src/main/resources/db/migration` 后，于 `apps/backend/` 执行 `./gradlew jooqCodegen`（PowerShell 使用 `.\gradlew.bat jooqCodegen`）

当前 Repository 用 jOOQ `DSLContext` 手写字段访问，避免 Controller 直接碰数据库。后续表增多后，把 Repository 切到生成的 `Tables.*`。

## ID

主键使用 UUIDv7：时间有序、适合索引、方便以后同步和外部 Integration。实现见 `UuidV7`。

## 时间

数据库 `TIMESTAMPTZ` 存 UTC。Java 用 `Instant`。API 用 ISO-8601。
