# 目录与扩展约定

[文档索引](../README.md) · [项目首页](../../README.md)

## 1. 仓库边界

采用单仓库管理多个应用。应用集中在 `apps/`，文档集中在 `docs/`；每个应用保留自己的构建工具、依赖和配置。

```text
personal-ai-workspace/
├── apps/
│   ├── frontend/
│   │   ├── src/                 # React 应用源码
│   │   ├── public/              # 运行时直接访问的静态资源
│   │   ├── scripts/             # 前端资源生成等脚本
│   │   ├── e2e/                 # 前端端到端用例
│   │   ├── package.json
│   │   ├── pnpm-lock.yaml
│   │   └── README.md
│   └── backend/
│       ├── src/main/java/       # Java 业务与基础设施代码
│       ├── src/main/resources/ # Spring 配置与 Flyway SQL
│       ├── src/test/            # 后端用例
│       ├── gradle/              # Gradle Wrapper
│       ├── build.gradle.kts
│       └── README.md
├── docs/
│   ├── product/
│   ├── architecture/
│   ├── design/assets/
│   ├── development/
│   └── README.md
├── .codex/                     # 仓库工具配置
├── .vscode/                    # 本地编辑器配置（忽略提交）
├── .gitignore
├── docker-compose.yml          # 共享本地数据服务入口
└── README.md
```

根目录负责项目导航、仓库配置和共享启动入口。Compose 保留在根目录，便于直接执行 `docker compose up -d`，并保持现有数据挂载位置。

当前只有一个 JavaScript 应用，pnpm 的配置和锁文件保留在 `apps/frontend/`。Java 应用独立使用 Gradle。

## 2. 前端模块

源码根目录为 [`apps/frontend/src/`](../../apps/frontend/src/)。

| 目录 | 职责 |
| --- | --- |
| `app/` | 应用初始化、路由、布局、Provider 和全局配置 |
| `features/<feature>/` | 按业务组织页面、API、组件、校验规则和类型 |
| `shared/api/` | HTTP 客户端、通用错误处理、查询键等公共请求能力 |
| `shared/components/ui/` | 通用基础 UI 组件 |
| `shared/components/` | 多个业务模块复用的组合组件 |
| `shared/hooks/`、`shared/lib/`、`shared/utils/`、`shared/types/` | 跨业务的通用能力 |
| `stores/` | 客户端 UI 状态 |
| `styles/` | 全局样式与设计 Token |
| `assets/` | 被源码导入的应用素材 |
| `test/` | 前端公共测试初始化 |

新增功能优先放进 `features/<feature>/`，按需建立 `api/`、`pages/`、`components/`、`schemas/`、`hooks/`。只供一个模块使用的代码留在模块内，多模块需要时再抽到 `shared/`。

服务端数据由 TanStack Query 管理；客户端 UI 偏好放进 Zustand。页面组合可以依赖业务模块，通用基础组件保持与具体业务解耦。

## 3. 后端模块

源码包根目录为 [`com.example.workspace`](../../apps/backend/src/main/java/com/example/workspace/)。

- `auth`、`user`、`workspace`、`project`、`task`、`note`、`dashboard`：业务模块。
- `common`：公共 API 结构、异常、安全上下文与通用工具。
- `infrastructure`：配置、认证基础设施、Redis 等技术实现。

业务模块按当前工程约定组织，按需创建子目录：

```text
<module>/
├── controller/      # HTTP 请求与响应
├── application/     # 用例编排、事务与业务服务
├── domain/          # 领域对象与枚举
├── dto/             # 请求与响应结构
└── repository/      # 数据访问
```

后续 File、Search、AI 等业务优先在此增加同级模块。Controller 调用应用服务，Repository 集中处理数据库访问；Workspace 权限与数据隔离沿用现有公共入口。

数据库迁移留在 [`apps/backend/src/main/resources/db/migration/`](../../apps/backend/src/main/resources/db/migration/)，与后端一起版本管理。jOOQ 生成代码、Gradle 缓存和构建产物留在应用目录并由 Git 忽略。

## 4. 扩展时放在哪里

| 新增内容 | 放置位置 | 何时创建 |
| --- | --- | --- |
| 新页面、新业务能力 | 现有应用的 `features/` 或 Java 业务包 | 日常功能开发 |
| 独立管理端、移动端、Worker | `apps/<name>/` | 需要独立构建或运行时 |
| 多个 JS 应用共用的 UI / SDK | `packages/<name>/` | 出现实际跨应用复用时，再建立根 pnpm workspace 并统一锁文件 |
| 多个 Java 应用共用的模块 | `packages/<name>/` | 出现实际复用时，再由 Gradle 多项目或复合构建管理 |
| 部署清单、网关或监控配置 | `infra/<tool-or-environment>/` | 有具体部署需求时；同步维护根 Compose 入口 |
| 跨应用维护脚本 | `scripts/` | 脚本确实服务整个仓库时 |
| 新版本规划、架构决策、开发说明 | `docs/` 下对应分类 | 新增文档时更新索引 |

`packages/`、`infra/`、根 `scripts/` 等目录按实际内容创建。应用内部脚本、配置、资源和依赖继续放在所属应用内。

## 5. 资源与文档

- 设计参考图放在 `docs/design/assets/`，与设计规范一起维护。
- 前端运行时素材放在 `apps/frontend/src/assets/` 或 `apps/frontend/public/`，由构建与资源加载方式决定位置。
- 文档图片与应用素材各自归属清楚，业务代码不从 `docs/` 加载资源。
- 文档具体命名、分类和入口规则见[文档索引](../README.md)。

## 6. 调整目录时同步维护

移动应用或文档后，更新根与应用 README、文档索引、命令的工作目录，以及 Git 忽略规则。工具配置中的应用路径也要同步更新，包括 `.codex/config.toml` 与本地 `.vscode/settings.json`。

当前 ReactBits MCP 配置使用本机绝对路径，克隆到其他位置时需同步调整。已有终端或 IDE 若以旧应用目录打开，应重新打开新位置。
