# AI Personal Workspace

个人 AI 工作与知识管理平台，以 Workspace 组织项目、任务、笔记及后续 AI 能力。

前端使用 React + TypeScript + Vite，后端使用 Java 25 + Spring Boot，数据服务使用 PostgreSQL + Redis。

## 项目目录

```text
personal-ai-workspace/
├── apps/
│   ├── frontend/          # Web 应用、依赖、配置和应用内脚本
│   └── backend/           # Spring Boot 应用、Gradle 和数据库迁移
├── docs/
│   ├── README.md          # 文档索引
│   ├── product/           # 产品范围、版本规划
│   ├── architecture/      # 工程结构、模块边界
│   ├── design/            # UI 规范与参考素材
│   └── development/       # 前后端开发指南
├── docker-compose.yml    # 本地 PostgreSQL / Redis 启动入口
└── README.md             # 项目入口
```

新增应用放在 `apps/<name>/`；新增业务功能放进现有应用的业务模块。详细规则见[目录与扩展约定](docs/architecture/repository-layout.md)。

## 本地启动

准备 Docker Compose、JDK 25、Node.js 和 pnpm。以下示例使用 PowerShell，每组命令都从仓库根目录开始，在独立终端执行。

### 1. 数据服务

```powershell
docker compose up -d
```

当前 Compose 将数据持久化到 `E:/docker-data/personal-ai-workspace/`，迁移到其他机器时按实际环境调整 [docker-compose.yml](docker-compose.yml) 中的宿主机路径。

### 2. 后端

```powershell
cd apps/backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

后端默认地址：[http://localhost:8080](http://localhost:8080)。本地 API 文档：[http://localhost:8080/scalar](http://localhost:8080/scalar)。

### 3. 前端

```powershell
cd apps/frontend
pnpm install
pnpm dev
```

前端默认地址：[http://localhost:5173](http://localhost:5173)，通过 Vite 将 `/api` 代理到后端。

## 文档入口

| 内容 | 位置 |
| --- | --- |
| 全部文档与存放规则 | [文档索引](docs/README.md) |
| 工程目录、模块边界与扩展方式 | [目录与扩展约定](docs/architecture/repository-layout.md) |
| V0.1 产品与技术规划 | [版本规划](docs/product/v0.1-plan.md) |
| V0.2 二期规划、任务与实施计划 | [二期文档总入口](docs/product/v0.2-plan.md) |
| RustFS 接入与 S3 API 二次封装 | [存储封装方案](docs/architecture/v0.2-s3-storage.md) |
| UI 规范与参考图 | [UI 设计系统](docs/design/ui-design-system.md) |
| 前端开发、页面与组件说明 | [前端开发指南](docs/development/frontend.md) |
| 后端开发、API 与认证说明 | [后端开发指南](docs/development/backend.md) |

各应用目录中的 README 提供就近入口，完整说明统一维护在 `docs/`。
