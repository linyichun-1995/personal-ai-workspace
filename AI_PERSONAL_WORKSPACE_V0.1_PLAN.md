# AI Personal Workspace V0.1 产品与技术规划

> 文档状态：Draft 1.0  
> 版本范围：V0.1  
> 核心模块：User、Workspace、Project、Task、Note  
> 产品定位：个人 AI 工作与知识管理平台

---

## 1. 文档目标

本文档用于指导 AI Personal Workspace V0.1 的产品设计、技术设计、任务拆分、开发联调和验收。

V0.1 的核心目标不是一次性实现完整的 AI 工作平台，而是先建立稳定的业务底座：

1. 用户可以安全地注册、登录并管理个人资料。
2. 每个用户拥有一个默认 Workspace，并且所有业务数据都严格归属于 Workspace。
3. 用户可以通过 Project 组织工作。
4. 用户可以通过 Task 管理执行状态和时间安排。
5. 用户可以通过 Note 记录 Markdown 内容，并关联 Project 或 Task。
6. 数据模型和接口为后续 File、Search、AI Chat、RAG、Agent 留出扩展空间。

---

## 2. 产品愿景与版本路线

### 2.1 产品愿景

AI Personal Workspace 是一个以 Workspace 为边界的个人工作与知识管理平台，将项目、任务、笔记、文件和 AI 能力整合在统一上下文中。

平台未来需要让 AI 不只是聊天工具，还能够理解用户的项目、任务、笔记与文件，并在授权范围内检索信息和执行操作。

### 2.2 版本路线

| 版本 | 核心范围 | 目标 |
|---|---|---|
| V0.1 | User、Workspace、Project、Task、Note | 建立用户、数据归属和核心工作流 |
| V0.2 | File、Tag、Keyword Search | 建立内容入口和统一检索能力 |
| V0.3 | AI Chat | 建立多会话、流式输出和上下文选择 |
| V0.4 | RAG、Semantic Search | 建立个人知识库和来源引用 |
| V0.5 | AI Agent、Automation | 让 AI 查询、创建和更新业务数据 |

### 2.3 V0.1 成功标准

V0.1 完成后，用户应当能独立完成以下闭环：

```text
注册 / 登录
    ↓
进入默认 Workspace
    ↓
创建 Project
    ↓
创建并推进 Task
    ↓
创建 Markdown Note
    ↓
将 Note 关联到 Project 或 Task
    ↓
在 Dashboard 查看当前工作的聚合摘要
```

---

## 3. V0.1 范围

### 3.1 本期包含

#### User

- 邮箱和密码注册
- 邮箱和密码登录
- 退出登录
- 当前登录状态恢复
- 查看和编辑个人资料
- 修改密码
- 安全的密码存储和会话管理

#### Workspace

- 注册时自动创建个人 Workspace
- 查看当前 Workspace
- 编辑 Workspace 名称和基础设置
- Workspace 成员与角色模型
- 当前版本默认只有一个用户可见 Workspace
- 数据访问强制进行 Workspace 隔离

#### Project

- 创建、查看、编辑项目
- 项目列表和详情
- 项目状态、优先级、起止日期
- 项目归档和恢复
- 查看项目关联的任务与笔记

#### Task

- 创建、查看、编辑、删除任务
- Todo / Doing / Done 状态流转
- 优先级、开始时间、截止时间
- 可选关联项目
- 一级子任务
- 列表视图
- 看板视图
- 今日、逾期、即将到期筛选
- 同状态内手动排序

#### Note

- 创建、查看、编辑、删除笔记
- Markdown 纯文本存储
- 编辑器预览
- 自动保存
- 收藏 / 取消收藏
- 可选关联项目
- 可选关联任务
- 最近编辑列表

#### Dashboard

- 今日到期任务
- 逾期未完成任务
- 即将到期任务
- 进行中项目
- 最近编辑笔记
- 基础数量统计

### 3.2 本期不包含

以下能力明确延后，避免扩大 V0.1 的实施范围：

- 邮箱验证、忘记密码和第三方登录
- 多人协作邀请及复杂权限配置
- 用户可切换多个 Workspace 的完整交互
- 文件上传、预览、对象存储与文档解析
- Tag 管理；V0.1 不在 Project、Task、Note 上保存临时字符串标签
- 全局关键词搜索和全文搜索
- AI Chat、模型配置和流式生成
- Embedding、向量数据库和 RAG
- AI Agent 和自动化工作流
- 评论、@提及、通知中心
- 完整 Activity Timeline 和审计后台
- 富文本或多人实时协同编辑
- 循环任务、任务依赖、甘特图和日历视图
- 离线编辑和跨设备冲突合并

### 3.3 重要范围决策

1. **Workspace 从第一天进入所有业务表。** 即使 V0.1 的用户只有一个默认 Workspace，也不能将数据直接只挂到 User 下。
2. **归档与状态分开。** Project 的业务状态不包含 `ARCHIVED`，归档使用独立的 `archived_at` 字段。
3. **Note 保存原始 Markdown。** V0.1 不将 Markdown 转换后的 HTML 作为主数据。
4. **Tag 延后统一实现。** 避免 V0.1 先做临时字段，V0.2 再进行破坏性迁移。
5. **任务仅支持一级子任务。** V0.1 限制层级，降低查询、排序和看板交互复杂度。
6. **删除默认采用软删除。** 稳定的实体 ID 有利于未来审计、AI 引用和误删恢复。

---

## 4. 用户角色与权限边界

### 4.1 Workspace 角色

| 角色 | V0.1 用途 | 权限 |
|---|---|---|
| OWNER | 默认个人 Workspace 所有者 | 查看和修改 Workspace；管理全部业务数据 |
| MEMBER | 为后续多人 Workspace 预留 | V0.1 不提供邀请入口，可访问被授权 Workspace 内数据 |

V0.1 可以在数据库和服务层支持 `OWNER`、`MEMBER`，但前端暂不实现成员管理。

### 4.2 授权原则

- 客户端传入的 `workspaceId` 不能直接作为授权依据。
- 每次访问 Project、Task、Note 前，都必须校验当前用户是否属于对应 Workspace。
- 通过实体 ID 查询数据时，查询条件必须同时包含 `id` 和 `workspace_id`。
- 不允许通过猜测 UUID 访问其他 Workspace 的数据。
- OWNER 才能修改 Workspace 设置。
- 软删除的数据默认不出现在查询结果中。

### 4.3 数据隔离示例

错误方式：

```sql
SELECT * FROM tasks WHERE id = :taskId;
```

正确方式：

```sql
SELECT *
FROM tasks
WHERE id = :taskId
  AND workspace_id = :workspaceId
  AND deleted_at IS NULL;
```

---

## 5. 核心用户场景

### 5.1 新用户开始使用

1. 用户使用邮箱和密码注册。
2. 系统在同一事务内创建 User、默认 Workspace 和 OWNER 成员关系。
3. 系统创建登录会话。
4. 用户进入 Dashboard。
5. 空状态引导用户创建第一个 Project 或 Task。

### 5.2 组织一个项目

1. 用户创建 Project，设置名称、描述、状态、优先级和日期。
2. 用户在 Project 详情中创建 Task。
3. 用户创建项目 Note，记录背景、方案或会议内容。
4. Project 详情聚合展示项目资料、Task 和 Note。
5. 项目结束后，用户将状态设为 Completed 或将项目归档。

### 5.3 管理每日任务

1. 用户进入 Tasks 列表或看板。
2. 用户按状态、优先级、项目和日期范围筛选。
3. 用户将 Task 从 Todo 移到 Doing，再移动到 Done。
4. 系统在进入 Done 时记录完成时间。
5. Dashboard 将今天、逾期和即将到期任务进行聚合。

### 5.4 记录并自动保存笔记

1. 用户新建 Note，系统立即创建草稿并返回 ID。
2. 用户编辑标题和 Markdown 正文。
3. 前端在停止输入一段时间后自动保存。
4. 保存成功后更新本地版本和 `updatedAt`。
5. 网络失败时保留未保存内容，并允许重试。

---

## 6. 信息架构与页面规划

### 6.1 主导航

```text
Workspace
├── Dashboard
├── Projects
├── Tasks
├── Notes
└── Settings
    ├── Profile
    ├── Workspace
    └── Security
```

V0.2 起可增加 Files、Search 和 Tags；V0.3 起增加 AI Chat。

### 6.2 路由建议

| 路由 | 页面 | 登录要求 |
|---|---|---|
| `/login` | 登录 | 否 |
| `/register` | 注册 | 否 |
| `/` | 登录态重定向到 Dashboard | 是 |
| `/dashboard` | 工作台 | 是 |
| `/projects` | 项目列表 | 是 |
| `/projects/new` | 新建项目 | 是 |
| `/projects/$projectId` | 项目详情 | 是 |
| `/projects/$projectId/edit` | 编辑项目 | 是 |
| `/tasks` | 任务列表 | 是 |
| `/tasks/board` | 任务看板 | 是 |
| `/tasks/$taskId` | 任务详情 | 是 |
| `/notes` | 笔记列表 | 是 |
| `/notes/$noteId` | 笔记编辑器 | 是 |
| `/settings/profile` | 个人资料 | 是 |
| `/settings/workspace` | Workspace 设置 | 是 |
| `/settings/security` | 密码与会话 | 是 |

### 6.3 页面要点

#### Dashboard

- 顶部显示问候语和当前日期。
- 展示今日任务、逾期任务、进行中项目和最近笔记。
- 每个区块提供跳转到完整列表的入口。
- 无数据时提供明确的首次创建操作。

#### Projects

- 默认显示未归档项目。
- 支持按状态和优先级筛选。
- 支持按更新时间、优先级、开始时间排序。
- 归档项目通过独立筛选项显示。

#### Project Detail

- 展示项目基础信息、状态、优先级和日期。
- 分区展示 Task 和 Note。
- 可在当前项目上下文内快捷创建 Task 或 Note。
- 归档项目默认只读，并提供恢复入口。

#### Tasks List

- 支持状态、优先级、项目和日期条件筛选。
- 支持今日、逾期、即将到期快捷视图。
- 父任务可展开显示子任务。
- 支持批量状态更新可以延后，不作为 V0.1 验收项。

#### Tasks Board

- 固定 Todo、Doing、Done 三列。
- 拖拽可以更新状态和排序。
- 筛选条件与列表视图共用 URL 查询参数。
- 子任务不作为独立卡片显示，展示在父任务卡片内。

#### Notes

- 左侧为笔记列表，右侧为编辑区域；窄屏改为独立页面。
- 列表显示标题、摘要、收藏状态和最近更新时间。
- 编辑器支持编辑 / 预览切换。
- 顶部明确显示保存中、已保存或保存失败状态。

---

## 7. 技术架构

### 7.1 总体架构

```text
React SPA
  │
  │ HTTPS / JSON
  ▼
Spring Boot API
  ├── PostgreSQL：业务主数据
  ├── Redis：登录会话、限流、短期缓存
  └── Object Storage：V0.2 接入

Python AI Service：V0.3/V0.4 接入，V0.1 不部署也不依赖
```

### 7.2 前端技术栈

- React
- TypeScript
- Vite
- TanStack Router
- TanStack Query
- 表单方案建议：React Hook Form + Zod
- Markdown 建议：编辑器采用可替换适配层，渲染采用受控 Markdown Renderer
- 拖拽建议：dnd-kit
- 测试建议：Vitest + React Testing Library + Playwright

### 7.3 后端技术栈

- Spring Boot
- Spring Web
- Spring Security
- Spring Validation
- Spring Data JPA 或 jOOQ（二选一并保持一致）
- PostgreSQL
- Redis + Spring Session
- Flyway
- OpenAPI 3
- 测试建议：JUnit 5 + Testcontainers

### 7.4 身份认证建议

V0.1 推荐使用服务端会话，而不是在浏览器持久化 JWT：

- 登录成功后返回 `HttpOnly`、`Secure`、`SameSite=Lax` Cookie。
- Session 存储在 Redis，支持主动退出和服务端失效。
- 浏览器端不读取认证凭证。
- 修改数据的请求开启 CSRF 防护。
- 同一站点部署前端和 API，可降低 Cookie 与 CORS 配置复杂度。
- 后续开放移动端或第三方 API 时，再补充 OAuth2 / API Token。

### 7.5 模块边界

后端第一阶段建议采用模块化单体，不要过早拆分微服务：

```text
backend
├── auth
├── user
├── workspace
├── project
├── task
├── note
├── dashboard
├── shared
└── infrastructure
```

模块内可按以下结构组织：

```text
project
├── api
├── application
├── domain
└── infrastructure
```

原则：业务规则位于 application/domain 层，Controller 不直接拼装数据库查询。

---

## 8. 数据模型

### 8.1 通用约定

- 主键使用 UUID。
- 时间统一以 UTC 存储，API 使用 ISO 8601。
- 数据库字段使用 `snake_case`。
- API JSON 使用 `camelCase`。
- 所有 Workspace 业务表必须包含 `workspace_id`。
- 核心业务表包含 `created_at`、`updated_at`、`version`。
- Project、Task、Note 使用 `deleted_at` 实现软删除。
- 所有用户输入文本在输出到 HTML 时进行转义或消毒。
- `version` 用于乐观锁和避免自动保存覆盖新内容。

### 8.2 实体关系

```text
User ──< WorkspaceMember >── Workspace
                                  │
                                  ├──< Project
                                  │      ├──< Task
                                  │      └──< Note
                                  │
                                  ├──< Task ──< Subtask
                                  │      └──< NoteTask >── Note
                                  │
                                  └──< Note
```

### 8.3 users

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | UUID | PK | 用户 ID |
| email | VARCHAR(320) | NOT NULL | 原始邮箱 |
| email_normalized | VARCHAR(320) | UNIQUE, NOT NULL | 小写和规范化后的邮箱 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希 |
| display_name | VARCHAR(100) | NOT NULL | 显示名称 |
| avatar_url | VARCHAR(1000) | NULL | V0.1 可仅支持 URL |
| locale | VARCHAR(20) | NOT NULL | 默认 `zh-CN` |
| timezone | VARCHAR(50) | NOT NULL | 默认取注册端时区 |
| status | VARCHAR(20) | NOT NULL | `ACTIVE`、`DISABLED` |
| created_at | TIMESTAMPTZ | NOT NULL | 创建时间 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新时间 |
| version | BIGINT | NOT NULL | 乐观锁版本 |

### 8.4 workspaces

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | UUID | PK | Workspace ID |
| name | VARCHAR(120) | NOT NULL | 名称 |
| slug | VARCHAR(80) | UNIQUE, NOT NULL | 后续多 Workspace 路由预留 |
| type | VARCHAR(20) | NOT NULL | V0.1 为 `PERSONAL` |
| timezone | VARCHAR(50) | NOT NULL | 用于“今日”和截止时间计算 |
| week_starts_on | SMALLINT | NOT NULL | 1 表示周一 |
| created_at | TIMESTAMPTZ | NOT NULL | 创建时间 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新时间 |
| version | BIGINT | NOT NULL | 乐观锁版本 |

### 8.5 workspace_members

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | UUID | PK | 成员关系 ID |
| workspace_id | UUID | FK, NOT NULL | Workspace |
| user_id | UUID | FK, NOT NULL | User |
| role | VARCHAR(20) | NOT NULL | `OWNER`、`MEMBER` |
| joined_at | TIMESTAMPTZ | NOT NULL | 加入时间 |

唯一约束：`UNIQUE(workspace_id, user_id)`。

### 8.6 projects

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | UUID | PK | Project ID |
| workspace_id | UUID | FK, NOT NULL | 数据归属 |
| name | VARCHAR(200) | NOT NULL | 项目名称 |
| description | TEXT | NULL | 项目描述 |
| status | VARCHAR(20) | NOT NULL | `PLANNED`、`ACTIVE`、`ON_HOLD`、`COMPLETED` |
| priority | VARCHAR(20) | NOT NULL | `NONE`、`LOW`、`MEDIUM`、`HIGH`、`URGENT` |
| start_date | DATE | NULL | 开始日期 |
| end_date | DATE | NULL | 计划结束日期 |
| color | VARCHAR(20) | NULL | UI 标识色 |
| archived_at | TIMESTAMPTZ | NULL | 归档时间 |
| created_by | UUID | FK, NOT NULL | 创建人 |
| created_at | TIMESTAMPTZ | NOT NULL | 创建时间 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新时间 |
| deleted_at | TIMESTAMPTZ | NULL | 软删除时间 |
| version | BIGINT | NOT NULL | 乐观锁版本 |

业务约束：`end_date >= start_date`，任一日期为空时不校验。

### 8.7 tasks

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | UUID | PK | Task ID |
| workspace_id | UUID | FK, NOT NULL | 数据归属 |
| project_id | UUID | FK, NULL | 可选所属项目 |
| parent_task_id | UUID | FK, NULL | 可选父任务 |
| title | VARCHAR(300) | NOT NULL | 标题 |
| description | TEXT | NULL | Markdown 描述 |
| status | VARCHAR(20) | NOT NULL | `TODO`、`DOING`、`DONE` |
| priority | VARCHAR(20) | NOT NULL | `NONE`、`LOW`、`MEDIUM`、`HIGH`、`URGENT` |
| start_at | TIMESTAMPTZ | NULL | 开始时间 |
| due_at | TIMESTAMPTZ | NULL | 截止时间 |
| completed_at | TIMESTAMPTZ | NULL | 完成时间 |
| position | BIGINT | NOT NULL | 看板和列表排序 |
| created_by | UUID | FK, NOT NULL | 创建人 |
| created_at | TIMESTAMPTZ | NOT NULL | 创建时间 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新时间 |
| deleted_at | TIMESTAMPTZ | NULL | 软删除时间 |
| version | BIGINT | NOT NULL | 乐观锁版本 |

业务约束：

- 父任务和子任务必须属于同一个 Workspace。
- 如果父任务属于某个 Project，子任务必须属于同一个 Project。
- V0.1 不允许子任务继续拥有子任务。
- `due_at >= start_at`，任一时间为空时不校验。
- 状态变为 `DONE` 时设置 `completed_at`。
- 从 `DONE` 变为其他状态时清空 `completed_at`。
- 删除父任务时，必须由用户选择同时删除子任务，或先解除子任务；V0.1 推荐整体软删除。

### 8.8 notes

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | UUID | PK | Note ID |
| workspace_id | UUID | FK, NOT NULL | 数据归属 |
| project_id | UUID | FK, NULL | 可选所属项目 |
| title | VARCHAR(300) | NOT NULL | 标题，可默认“无标题” |
| content | TEXT | NOT NULL | 原始 Markdown |
| excerpt | VARCHAR(500) | NULL | 服务端生成的纯文本摘要 |
| is_favorite | BOOLEAN | NOT NULL | 是否收藏 |
| created_by | UUID | FK, NOT NULL | 创建人 |
| created_at | TIMESTAMPTZ | NOT NULL | 创建时间 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新时间 |
| deleted_at | TIMESTAMPTZ | NULL | 软删除时间 |
| version | BIGINT | NOT NULL | 自动保存冲突控制 |

### 8.9 note_tasks

Note 和 Task 使用多对多关系，避免未来会议笔记或方案笔记只能关联一个任务。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| note_id | UUID | FK, NOT NULL | Note ID |
| task_id | UUID | FK, NOT NULL | Task ID |
| created_at | TIMESTAMPTZ | NOT NULL | 关联时间 |

主键：`PRIMARY KEY(note_id, task_id)`。

业务约束：Note 和 Task 必须属于同一个 Workspace；若二者都有关联 Project，则 Project 必须一致。

### 8.10 推荐索引

```sql
CREATE INDEX idx_projects_workspace_status
    ON projects(workspace_id, status)
    WHERE deleted_at IS NULL AND archived_at IS NULL;

CREATE INDEX idx_tasks_workspace_status_position
    ON tasks(workspace_id, status, position)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tasks_workspace_due_at
    ON tasks(workspace_id, due_at)
    WHERE deleted_at IS NULL AND status <> 'DONE';

CREATE INDEX idx_tasks_project_id
    ON tasks(project_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tasks_parent_task_id
    ON tasks(parent_task_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notes_workspace_updated_at
    ON notes(workspace_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notes_project_id
    ON notes(project_id)
    WHERE deleted_at IS NULL;
```

---

## 9. API 设计

### 9.1 通用约定

- 基础路径：`/api/v1`
- 数据格式：`application/json`
- 时间格式：ISO 8601，例如 `2026-09-13T08:30:00Z`
- 日期格式：`YYYY-MM-DD`
- 列表分页：游标分页优先；V0.1 允许先使用 `page` + `size`
- 默认分页大小：20，最大 100
- 所有修改请求支持 `version` 字段进行乐观锁校验
- DELETE 对核心实体执行软删除
- API 不返回数据库异常和堆栈信息

统一错误结构：

```json
{
  "code": "TASK_VERSION_CONFLICT",
  "message": "任务已在其他位置更新，请刷新后重试",
  "fieldErrors": [],
  "traceId": "01J..."
}
```

建议状态码：

| 状态码 | 场景 |
|---|---|
| 200 | 查询或更新成功 |
| 201 | 创建成功 |
| 204 | 删除、归档等无正文操作成功 |
| 400 | 请求格式或业务参数错误 |
| 401 | 未登录或会话失效 |
| 403 | 已登录但无 Workspace 权限 |
| 404 | 资源不存在或不属于当前 Workspace |
| 409 | 唯一性冲突或版本冲突 |
| 422 | 可识别但违反业务规则 |
| 429 | 请求过于频繁 |

### 9.2 Auth API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/register` | 注册并创建默认 Workspace |
| POST | `/auth/login` | 登录并创建 Session |
| POST | `/auth/logout` | 退出并销毁当前 Session |
| GET | `/auth/session` | 恢复当前登录态 |

注册请求示例：

```json
{
  "email": "user@example.com",
  "password": "example-password",
  "displayName": "Alex",
  "timezone": "Asia/Shanghai"
}
```

注册成功响应应包含用户、默认 Workspace 和 CSRF 所需信息，但不返回密码字段。

### 9.3 User API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/users/me` | 获取个人资料 |
| PATCH | `/users/me` | 更新显示名称、头像、语言和时区 |
| PUT | `/users/me/password` | 修改密码并使其他 Session 失效 |

### 9.4 Workspace API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/workspaces` | 获取当前用户可访问 Workspace |
| GET | `/workspaces/{workspaceId}` | 获取 Workspace 详情 |
| PATCH | `/workspaces/{workspaceId}` | 更新 Workspace 设置 |

即使 V0.1 只有一个默认 Workspace，前端也通过 Workspace API 获取，不将 Workspace 写死在客户端。

### 9.5 Project API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/workspaces/{workspaceId}/projects` | 项目列表和筛选 |
| POST | `/workspaces/{workspaceId}/projects` | 创建项目 |
| GET | `/workspaces/{workspaceId}/projects/{projectId}` | 项目详情 |
| PATCH | `/workspaces/{workspaceId}/projects/{projectId}` | 更新项目 |
| DELETE | `/workspaces/{workspaceId}/projects/{projectId}` | 软删除项目 |
| POST | `/workspaces/{workspaceId}/projects/{projectId}/archive` | 归档项目 |
| POST | `/workspaces/{workspaceId}/projects/{projectId}/restore` | 恢复项目 |

列表查询参数建议：

```text
status=ACTIVE
priority=HIGH
archived=false
sort=updatedAt,desc
page=0
size=20
```

删除 Project 时不级联删除 Task 和 Note。若项目下仍有内容，V0.1 推荐返回业务错误并引导用户先归档项目；真正的级联删除策略留到回收站功能设计时确定。

### 9.6 Task API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/workspaces/{workspaceId}/tasks` | 任务列表和筛选 |
| POST | `/workspaces/{workspaceId}/tasks` | 创建任务或子任务 |
| GET | `/workspaces/{workspaceId}/tasks/{taskId}` | 任务详情 |
| PATCH | `/workspaces/{workspaceId}/tasks/{taskId}` | 更新任务 |
| DELETE | `/workspaces/{workspaceId}/tasks/{taskId}` | 软删除任务 |
| PATCH | `/workspaces/{workspaceId}/tasks/{taskId}/status` | 更新状态 |
| PATCH | `/workspaces/{workspaceId}/tasks/{taskId}/position` | 更新看板排序 |

筛选参数建议：

```text
status=TODO,DOING
priority=HIGH,URGENT
projectId=<uuid>
parent=ROOT
due=TODAY
sort=dueAt,asc
page=0
size=50
```

`due` 支持：

- `TODAY`
- `OVERDUE`
- `UPCOMING`
- `NO_DUE_DATE`

定义：

- Today：按 Workspace 时区，`due_at` 落在今天范围内且状态不是 Done。
- Overdue：`due_at` 早于今天起点且状态不是 Done。
- Upcoming：从明天起到未来 7 天内且状态不是 Done。

排序更新请求示例：

```json
{
  "status": "DOING",
  "beforeTaskId": "8d0c...",
  "afterTaskId": "f734...",
  "version": 4
}
```

服务端负责计算新的 `position`，客户端不直接决定任意数值。位置间隔不足时，服务端对该状态列重新编号。

### 9.7 Note API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/workspaces/{workspaceId}/notes` | 笔记列表 |
| POST | `/workspaces/{workspaceId}/notes` | 创建空白或带初始内容的 Note |
| GET | `/workspaces/{workspaceId}/notes/{noteId}` | 获取完整 Note |
| PATCH | `/workspaces/{workspaceId}/notes/{noteId}` | 更新标题、正文、收藏或关联 |
| DELETE | `/workspaces/{workspaceId}/notes/{noteId}` | 软删除 Note |
| PUT | `/workspaces/{workspaceId}/notes/{noteId}/tasks` | 替换关联任务集合 |

笔记列表默认不返回完整 `content`，只返回 `excerpt`，减少负载。

自动保存请求示例：

```json
{
  "title": "项目方案",
  "content": "# 背景\n\n正文内容……",
  "projectId": "4b5f...",
  "taskIds": ["8d0c..."],
  "version": 12
}
```

发生版本冲突时返回 `409 NOTE_VERSION_CONFLICT`，并附带服务端最新 `version` 和 `updatedAt`。V0.1 前端不自动合并正文，而是保留本地草稿并提示用户刷新或复制内容后处理。

### 9.8 Dashboard API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/workspaces/{workspaceId}/dashboard` | 一次返回 Dashboard 所需摘要 |

响应建议：

```json
{
  "taskCounts": {
    "today": 3,
    "overdue": 2,
    "upcoming": 5
  },
  "todayTasks": [],
  "overdueTasks": [],
  "activeProjects": [],
  "recentNotes": []
}
```

Dashboard 使用聚合专用查询，避免前端并发请求多个大列表再自行统计。

---

## 10. 关键业务规则

### 10.1 注册

- 邮箱比较使用 `email_normalized`。
- 密码最少 10 位，允许密码管理器生成的长密码。
- 密码使用 Argon2id 或 BCrypt 哈希，禁止明文和可逆加密。
- User、Workspace、WorkspaceMember 必须在同一数据库事务中创建。
- 注册失败时不得留下不完整 Workspace。

### 10.2 Project

- 项目名称去除首尾空格后不能为空。
- 归档项目默认从项目列表、任务选择器和 Note 选择器中隐藏。
- 归档不改变 Project 的业务状态。
- 归档项目下的现有 Task 和 Note 仍可读取。
- 若用户恢复项目，原状态保持不变。

### 10.3 Task

- Task 可以不属于 Project，作为 Workspace 级个人任务存在。
- 子任务继承父任务的 Project，不能单独移动到其他 Project。
- 父任务完成时不自动完成所有子任务，若仍有未完成子任务需二次确认。
- 子任务全部完成时，不自动完成父任务。
- Done 任务不计入逾期和即将到期。
- 截止时间判断以 Workspace 时区为准，不以浏览器临时时区为准。

### 10.4 Note

- 新建 Note 可使用“无标题”作为占位标题。
- `content` 允许为空字符串。
- 服务端从 Markdown 中去除语法标记后生成 `excerpt`。
- 自动保存只发送发生变化的字段。
- 切换页面前若保存仍未完成，前端应尝试完成保存并保留本地草稿。
- Markdown 中的原始 HTML 默认禁用；若启用，必须进行严格消毒以防 XSS。

---

## 11. 前端设计

### 11.1 推荐目录

```text
frontend/src
├── app
│   ├── router
│   ├── providers
│   └── layouts
├── features
│   ├── auth
│   ├── workspace
│   ├── projects
│   ├── tasks
│   ├── notes
│   └── dashboard
├── components
│   ├── ui
│   └── shared
├── lib
│   ├── api
│   ├── query
│   ├── validation
│   └── date
├── hooks
├── types
└── main.tsx
```

### 11.2 TanStack Router

- 使用文件路由或集中式类型安全路由，项目内保持一致。
- 根级登录态 Loader 调用 `/auth/session`。
- 受保护路由在进入页面前校验 Session。
- 列表筛选状态写入 URL Search Params，支持刷新和分享后恢复。
- 路由参数和 Search Params 使用 Schema 校验。

### 11.3 TanStack Query

Query Key 建议：

```ts
['session']
['workspaces']
['workspace', workspaceId]
['projects', workspaceId, filters]
['project', workspaceId, projectId]
['tasks', workspaceId, filters]
['task', workspaceId, taskId]
['notes', workspaceId, filters]
['note', workspaceId, noteId]
['dashboard', workspaceId]
```

缓存更新原则：

- 创建成功后，将新实体写入详情缓存，并使相关列表失效。
- 简单字段更新可以乐观更新；失败时回滚。
- 看板拖拽必须乐观更新，失败时恢复原顺序并提示。
- Note 自动保存不在每个按键后使列表失效。
- 登录状态 401 由统一 API 层处理，清理 Session 缓存并跳转登录页。

### 11.4 自动保存状态机

```text
SAVED
  └── 用户输入 → DIRTY
                    └── 停止输入 800ms → SAVING
                                           ├── 成功 → SAVED
                                           ├── 网络错误 → ERROR_RETRYABLE
                                           └── 版本冲突 → CONFLICT
```

建议行为：

- 输入防抖：800ms。
- 同一 Note 同时最多一个保存请求。
- 保存期间继续输入时，当前请求完成后立即再保存最新内容。
- 网络失败采用有限次数指数退避。
- 将未保存草稿按 Note ID 暂存在浏览器本地存储。
- 服务端成功保存后再清除对应本地草稿。

### 11.5 表单与错误反馈

- 前端 Schema 与后端约束保持一致，但后端仍是最终校验者。
- 字段错误显示在字段附近。
- 网络或系统错误使用页面级提示或 Toast。
- 删除、归档等操作提供明确结果反馈。
- 不用颜色作为状态的唯一表达方式。

---

## 12. 后端设计

### 12.1 请求处理链路

```text
Request
  → Authentication
  → Workspace Membership Check
  → Validation
  → Application Service
  → Domain Rules
  → Repository
  → Transaction Commit
  → Response DTO
```

### 12.2 事务边界

- 注册：User + Workspace + WorkspaceMember 一个事务。
- 创建子任务：父任务校验 + Task 创建一个事务。
- 更新 Task 状态：状态 + `completed_at` 一个事务。
- 更新 Note：正文 + 摘要 + Project/Task 关联一个事务。
- 归档 Project：只更新 Project，不批量改动关联实体。

### 12.3 DTO 与实体隔离

- API 不直接序列化 ORM Entity。
- Request DTO 负责格式校验。
- Application Service 负责权限和业务规则。
- Response DTO 明确控制可暴露字段。
- 枚举值使用稳定的大写字符串，不暴露数据库内部数值。

### 12.4 并发与幂等

- Project、Task、Note 更新使用乐观锁。
- 创建接口可接受 `Idempotency-Key`，防止网络重试创建重复实体。
- Note 自动保存必须携带 `version`。
- 看板排序由服务端序列化处理同一状态列的重排。

### 12.5 缓存策略

V0.1 只缓存收益明确且失效简单的数据：

- Redis 主要用于 Session 和限流。
- Workspace 成员关系可使用短 TTL 缓存。
- Project、Task、Note 列表不优先缓存，先依赖 PostgreSQL 索引。
- Dashboard 若查询压力明显，可增加 30～60 秒短缓存，并在相关实体变化后主动失效。

不要在 V0.1 为所有 Repository 默认增加缓存。

---

## 13. 安全与隐私

### 13.1 必须项

- 全站 HTTPS。
- Session Cookie 设置 `HttpOnly`、`Secure`、`SameSite=Lax`。
- 开启 CSRF 防护。
- 登录和注册接口限流。
- 密码使用 Argon2id 或 BCrypt。
- 不在日志中记录密码、Cookie、完整 Note 正文等敏感信息。
- Markdown 渲染防止 XSS。
- 所有数据库查询执行 Workspace 隔离。
- 错误响应不泄露“某邮箱是否存在”之外的额外账户信息。
- 修改密码后使其他 Session 失效。

### 13.2 基础限流建议

| 接口 | 建议限制 |
|---|---|
| 登录 | 每 IP 每分钟 10 次，并增加账户维度限制 |
| 注册 | 每 IP 每小时 10 次 |
| 修改密码 | 每用户每小时 5 次 |
| Note 自动保存 | 每用户每分钟 120 次 |
| 其他写接口 | 每用户每分钟 120 次 |

具体数值应通过实际使用数据调整，不作为硬编码业务常量。

---

## 14. 可观测性与运维底线

### 14.1 日志

- 每个请求生成 `traceId`。
- 结构化记录请求方法、路由、状态码、耗时、用户 ID 和 Workspace ID。
- 用户 ID、Workspace ID 仅记录内部 UUID。
- 禁止记录认证凭证和完整 Note 内容。
- 对登录失败、权限拒绝、版本冲突进行可统计记录。

### 14.2 指标

- API 请求量、错误率和 P95 延迟。
- 数据库连接池使用率和慢查询。
- Redis 可用性和命中情况。
- 登录成功 / 失败次数。
- Note 自动保存成功率和版本冲突率。
- 后台任务失败数；V0.1 即使没有业务队列，也应预留指标命名规范。

### 14.3 健康检查

- Liveness：应用进程是否存活。
- Readiness：PostgreSQL 和 Redis 是否可用。
- 健康检查接口不暴露凭证、连接串和内部异常详情。

### 14.4 备份

- PostgreSQL 每日自动备份。
- 定期验证恢复流程，而不只验证备份文件是否存在。
- 明确保留周期和恢复目标。
- V0.2 接入对象存储后，单独制定文件版本和备份策略。

---

## 15. 测试策略

### 15.1 后端单元测试

重点覆盖：

- 注册事务和邮箱唯一性。
- Workspace 成员权限判断。
- Project 日期和归档规则。
- Task 状态与 `completed_at` 一致性。
- Task 父子层级和 Project 一致性。
- Today、Overdue、Upcoming 时区边界。
- Note 版本冲突和关联完整性。

### 15.2 后端集成测试

使用真实 PostgreSQL 和 Redis 容器验证：

- 数据库迁移可以从空库完整执行。
- Session 创建、恢复和退出。
- 跨 Workspace 访问被拒绝。
- 软删除实体不会出现在默认查询中。
- 并发更新返回 409。
- 列表筛选、排序和分页正确。

### 15.3 前端组件测试

- 表单校验和错误提示。
- Task 状态更新与回滚。
- 筛选条件与 URL 同步。
- Note 自动保存状态机。
- 空状态、加载状态和错误状态。

### 15.4 端到端测试

至少覆盖以下主路径：

1. 注册后自动进入默认 Workspace。
2. 创建 Project 并在项目下创建 Task。
3. 在列表和看板之间切换，状态保持一致。
4. 创建 Note、自动保存、刷新后内容仍存在。
5. Note 关联 Project 和 Task。
6. Dashboard 正确显示今日和逾期任务。
7. 归档 Project 后默认列表不再显示，恢复后重新出现。
8. 一个用户不能访问另一个用户 Workspace 的实体。

---

## 16. 性能目标

以下为 V0.1 在正常网络和小规模个人数据下的目标：

| 指标 | 目标 |
|---|---|
| 普通 API P95 | 小于 300ms |
| Dashboard API P95 | 小于 500ms |
| 登录响应 P95 | 小于 800ms |
| Note 保存 P95 | 小于 400ms |
| 首屏可交互 | 小于 2.5s |
| 列表默认数据量 | 20～50 条 |

性能测试的基础数据集建议：每 Workspace 100 个 Project、10,000 个 Task、5,000 个 Note。

---

## 17. 开发阶段与任务拆分

### 阶段 0：工程基础

交付内容：

- 前后端工程初始化。
- 本地 PostgreSQL 和 Redis 环境。
- 环境变量与配置分层。
- Flyway 基线迁移。
- API 错误规范和统一响应。
- 前端路由、Query Provider 和基础布局。
- CI 中的格式检查、类型检查、测试和构建。

完成条件：主分支能够从干净环境启动，健康检查通过。

### 阶段 1：User + Workspace

交付内容：

- 注册、登录、退出和 Session 恢复。
- 注册时自动创建默认 Workspace。
- 个人资料设置。
- Workspace 设置。
- Workspace 权限校验中间层。

完成条件：两个独立用户的数据无法互相访问。

### 阶段 2：Project

交付内容：

- Project 数据库迁移和领域规则。
- Project CRUD API。
- 列表、详情、新建和编辑页面。
- 状态、优先级、日期、归档和恢复。

完成条件：项目完整生命周期通过端到端测试。

### 阶段 3：Task

交付内容：

- Task 和子任务数据模型。
- CRUD、筛选、状态和排序 API。
- 列表视图和看板视图。
- 今日、逾期和即将到期。
- Project 详情内的 Task 聚合。

完成条件：Task 状态、排序、日期筛选和子任务规则通过测试。

### 阶段 4：Note

交付内容：

- Note 与 NoteTask 数据模型。
- Note CRUD 和关联 API。
- Markdown 编辑与预览。
- 自动保存、草稿恢复和版本冲突提示。
- Project / Task 详情内的 Note 展示。

完成条件：断网重试、刷新恢复和并发冲突不会静默丢失正文。

### 阶段 5：Dashboard + 收尾

交付内容：

- Dashboard 聚合 API 和页面。
- 全局空状态、Loading、错误页和 404。
- 安全检查、性能检查和可访问性检查。
- 端到端回归。
- 部署、监控和备份说明。

完成条件：全部 V0.1 验收场景通过，可部署到测试环境。

---

## 18. V0.1 验收清单

### User 与认证

- [ ] 用户可以注册、登录和退出。
- [ ] 密码不会以明文或可逆形式存储。
- [ ] 刷新页面后登录态可以恢复。
- [ ] 修改密码后其他 Session 失效。
- [ ] 用户可以修改显示名称、头像、语言和时区。

### Workspace

- [ ] 注册成功后自动创建个人 Workspace。
- [ ] 用户可以查看和修改自己的 Workspace 设置。
- [ ] 非成员无法读取或修改 Workspace 数据。
- [ ] 所有 Project、Task、Note 均包含 Workspace 归属。

### Project

- [ ] 用户可以创建、查看和编辑 Project。
- [ ] 状态、优先级和日期校验正确。
- [ ] Project 可以归档和恢复。
- [ ] Project 详情展示关联 Task 和 Note。

### Task

- [ ] 用户可以创建、编辑和删除 Task。
- [ ] Task 可独立存在，也可关联 Project。
- [ ] 支持一级子任务且关系合法。
- [ ] 支持 Todo、Doing、Done 状态流转。
- [ ] 支持列表和看板视图。
- [ ] 看板拖拽失败时界面能够回滚。
- [ ] 今日、逾期、即将到期计算符合 Workspace 时区。

### Note

- [ ] 用户可以创建、编辑、收藏和删除 Note。
- [ ] 支持 Markdown 编辑和安全预览。
- [ ] 停止输入后能够自动保存。
- [ ] 保存失败不会静默丢失用户内容。
- [ ] 并发版本冲突有明确提示。
- [ ] Note 可以关联 Project 和多个 Task。

### Dashboard 与系统质量

- [ ] Dashboard 数据准确并可跳转到对应列表。
- [ ] 主要页面具有加载、空、错误三种状态。
- [ ] 核心 API 具有日志、指标和 traceId。
- [ ] 数据库迁移、自动化测试和构建进入 CI。
- [ ] PostgreSQL 备份与恢复流程经过验证。

---

## 19. 为后续版本预留的设计

### 19.1 V0.2：File、Tag、Search

- 新增统一 `tags` 和实体关联表，不修改核心实体为字符串数组。
- File 必须包含 `workspace_id`，并可关联 Project、Task、Note。
- 搜索结果使用统一资源引用：`resourceType + resourceId + workspaceId`。
- PostgreSQL 可先使用全文检索；规模扩大后再评估独立搜索引擎。

### 19.2 V0.3：AI Chat

- Chat Session 和 Message 必须归属于 Workspace。
- 上下文项使用统一资源引用，不将整份业务数据复制到 Message。
- 对话保存模型、Token 使用量、状态和错误信息。
- 流式输出与业务 API 分开设计，但共享认证和权限边界。

### 19.3 V0.4：RAG

- Note 与 File 都作为可索引资源。
- Chunk 保存来源资源 ID、版本、位置和内容哈希。
- 原文更新后按版本重新切分和 Embedding。
- 检索必须先按 Workspace 过滤，再进行相似度计算。
- 回答引用指向稳定实体 ID 和具体 Chunk。

### 19.4 V0.5：AI Agent

- Agent 只能通过受控 Tool API 访问业务数据，不直接连接数据库。
- 读操作与写操作分开授权。
- 创建、更新、删除等操作保留操作日志。
- 高风险写操作需要确认或可撤销机制。
- Tool 输入输出使用稳定、版本化的 Schema。

---

## 20. 风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 忽略 Workspace 隔离 | 后续多 Workspace 改造困难，存在越权风险 | 所有核心表和查询从 V0.1 强制包含 Workspace |
| Note 自动保存覆盖内容 | 用户数据丢失 | 乐观锁、本地草稿、冲突提示、串行保存 |
| 任务层级过深 | 查询和交互复杂 | V0.1 限制为一级子任务 |
| 看板排序频繁重排 | 性能下降或顺序冲突 | 稀疏 position + 必要时按列重排 |
| 过早拆微服务 | 交付慢、运维复杂 | V0.1 使用模块化单体 |
| 提前实现零散 Tag/Search | 数据模型反复迁移 | 按路线在 V0.2 统一实现 |
| 日期时区不一致 | 今日、逾期判断错误 | 以 Workspace 时区为唯一业务依据 |
| Markdown XSS | 账户和数据安全风险 | 禁止原始 HTML或严格消毒，增加安全测试 |

---

## 21. Definition of Done

一个功能只有同时满足以下条件才算完成：

- 产品交互和业务规则已实现。
- 正常、空、加载、错误和无权限状态均有处理。
- 后端完成输入校验和 Workspace 权限校验。
- 数据库迁移可重复在空环境执行。
- 关键业务规则有单元测试。
- 主路径有集成测试或端到端测试。
- API 文档已更新。
- 日志中不包含敏感数据。
- 代码通过格式、类型、测试和构建检查。
- 在测试环境完成验收并无 P0 / P1 缺陷。

---

## 22. 建议的首批迭代 Backlog

| 编号 | 工作项 | 优先级 | 依赖 |
|---|---|---|---|
| V01-001 | 初始化 React + TypeScript + Vite 工程 | P0 | 无 |
| V01-002 | 初始化 Spring Boot 工程 | P0 | 无 |
| V01-003 | 配置 PostgreSQL、Redis 和 Flyway | P0 | V01-002 |
| V01-004 | 建立统一错误、日志和 traceId | P0 | V01-002 |
| V01-005 | 实现 User、Workspace、Membership 迁移 | P0 | V01-003 |
| V01-006 | 实现注册和默认 Workspace 事务 | P0 | V01-005 |
| V01-007 | 实现登录、Session、退出和 CSRF | P0 | V01-006 |
| V01-008 | 实现前端登录态和受保护路由 | P0 | V01-001、V01-007 |
| V01-009 | 实现个人资料和 Workspace 设置 | P1 | V01-008 |
| V01-010 | 实现 Workspace 权限校验组件 | P0 | V01-005 |
| V01-011 | 实现 Project 迁移、领域规则和 API | P0 | V01-010 |
| V01-012 | 实现 Project 列表和详情页面 | P0 | V01-011 |
| V01-013 | 实现 Project 创建、编辑、归档和恢复 | P0 | V01-012 |
| V01-014 | 实现 Task 迁移、领域规则和 API | P0 | V01-011 |
| V01-015 | 实现 Task 列表和快捷筛选 | P0 | V01-014 |
| V01-016 | 实现 Task 看板、状态更新和排序 | P0 | V01-014 |
| V01-017 | 实现一级子任务 | P1 | V01-014 |
| V01-018 | 实现 Note 与 NoteTask 迁移和 API | P0 | V01-011、V01-014 |
| V01-019 | 实现 Markdown 编辑和预览 | P0 | V01-018 |
| V01-020 | 实现自动保存、本地草稿和冲突处理 | P0 | V01-019 |
| V01-021 | 实现 Dashboard 聚合 API 和页面 | P1 | V01-015、V01-020 |
| V01-022 | 完成 E2E、安全、性能和恢复测试 | P0 | 全部核心功能 |

---

## 23. 最终交付物

V0.1 应至少交付：

- 可部署的前端应用。
- 可部署的 Spring Boot API。
- PostgreSQL 数据库迁移脚本。
- Redis Session 和限流配置。
- OpenAPI 文档。
- 自动化测试与 CI 配置。
- 环境变量示例和本地启动说明。
- 测试环境部署说明。
- 数据库备份与恢复说明。
- V0.1 验收报告。

完成 V0.1 后，再进入 V0.2 的 File、Tag 和 Search 设计与实现。
