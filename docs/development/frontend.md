# 前端开发指南

React 19 + TypeScript + Vite + Tailwind CSS v4 + shadcn/ui 的工作台前端。

[文档索引](../README.md) · [前端应用](../../apps/frontend/README.md) · [UI 设计系统](../design/ui-design-system.md)

除非特别说明，下文源码、资源和配置路径均相对于 `apps/frontend/`。

## 开发

从仓库根目录执行：

```bash
cd apps/frontend
pnpm install
pnpm dev
```

开发服务器默认地址为 `http://localhost:5173`，`/api` 代理到 `http://localhost:8080`。可参考 [`.env.example`](../../apps/frontend/.env.example) 配置 `VITE_API_BASE_URL`。

以下命令在 `apps/frontend/` 内执行，需要时自行选择：

```bash
pnpm typecheck
pnpm lint
pnpm fmt
pnpm test -- --run
pnpm build
```

## 登录与注册交互

- `/login`：登录；`/register`：注册；`/forgot-password`：找回密码的前端反馈（本期不发送邮件）。
- 登录和注册会请求后端 `/api/v1/auth/*`，Access Token 保存在内存，Refresh Token 由 HttpOnly Cookie 维持。刷新页面会调用 `/auth/refresh` 恢复登录态。
- 设置页包含个人资料、工作空间、安全（改密）和外观。改密会使其他会话失效。
- 页面以最新蓝灰色机器人参考图为基准，原星空、极光与流光边框方案已移除。
- `src/features/auth/motion/auth-motion-controller.ts` 是所有动画的唯一状态源。输入框只派发事件，文字、SVG 连接与卡片消费统一 presentation；机器人接收同一状态对应的 Rive 输入。
- 提交依次连接项目 / 任务 / 笔记 / 文件 / AI：每步 260ms，总连接时间 1400ms；成功停留 650ms，再以 300ms 淡出进入 Dashboard。
- 机器人使用本地 `public/mascot/workspace-companion.riv`，状态机 `AuthCompanion`；通过 Rive View Model 的数字属性 `authState`（0–7 对应 idle/login/register/email/password/submitting/success/error；8 为 reduced-motion 静止态）及 `blink`（0/1）驱动。认证与眨眼使用独立状态机图层。
- 没有提供原始 Rive 资源，因此机器人分层画稿按效果图重建。`scripts/build-workspace-mascot.mjs` 是可复现源文件，`pnpm mascot:build` 会生成分层 SVG、Rive 文件并复制本地 WASM。无需外部 Rive 账户或 CDN；加载失败保留同造型 SVG，表单仍可使用。
- 支持系统减少动态效果、标签页隐藏时暂停、卸载清理定时器，以及六档屏宽验证。较窄屏幕减少轨道、节点与卡片，保留机器人。
- ReactBits 官方 registry 位于 [`components.json`](../../apps/frontend/components.json)，项目 MCP 配置位于仓库根目录 [`.codex/config.toml`](../../.codex/config.toml)，启动目录指向 `apps/frontend/`；当前页面动效采用 Motion + Rive + SVG。

验证命令：`pnpm typecheck`、`pnpm lint`、`pnpm fmt:check`、`pnpm exec vitest run`、`pnpm exec playwright test`、`pnpm build`。浏览器测试默认使用本机 Edge。

真实会话 E2E（`e2e/session.e2e.ts`）需要后端已启动，并且是包含资料 / Workspace API 的当前代码。在另一个终端从仓库根目录执行：

```powershell
docker compose up -d
cd apps/backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS / Linux 将 `.\gradlew.bat` 换成 `./gradlew`。

健康检查不通时该文件会 skip。若 8080 上仍是旧进程，资料与隔离用例会 skip，登录恢复与退出仍会跑。

## 工作台 UI 架构

- `src/styles/globals.css`：Primitive / Semantic / Component / Context 四层设计 Token 的落地入口。
- `src/app/layouts`：桌面、折叠与移动端共用的 App Shell。
- `src/shared/components`：页面容器、区块卡片、指标卡、状态徽章等跨 feature 组件。
- `src/shared/components/ui`：按钮、输入框、表格、复选框、进度条等基础组件。
- `src/features/*`：业务展示、业务类型和列定义；不重复实现通用 UI。

## 概览响应式布局

- 概览使用固定字号、图标、行高和卡片间距，按内容自然排布，不随视口宽高缩放或强制拉伸填屏。
- 以主内容区的实际可用宽度（扣除侧栏和页面留白）切换布局：760px 起 2 列、1180px 起 3 列、1760px 起 4 列、2800px 起 6 列；折叠侧栏时自动重新排版。
- 6 项指标按 2 / 3 / 6 列排列，8 个内容模块在所有尺寸均可访问，窄屏通过页面滚动查看。
- 概览通过 `src/features/dashboard/api/dashboard.ts` 请求后端数据；任务操作调用任务 API，再通过公共查询失效入口刷新相关摘要。

## DataTable

`src/shared/components/data-table.tsx` 与 `data-table-config.ts` 基于 TanStack React Table v9，已统一处理：

- 排序与全局搜索
- 客户端分页
- 当前页选择与选择计数
- 列显隐
- Sticky Header
- Loading / Empty 状态
- Compact / Comfortable 密度
- 语义表格结构和键盘焦点

业务模块使用 `createDataTableColumnHelper<T>()` 声明类型安全的列，再把列和数据传给 `DataTable`。参考 `src/features/task/components/task-table.tsx`。

服务端数据仍由 TanStack Query 管理；仅侧栏、命令面板等客户端 UI 偏好进入 Zustand。
