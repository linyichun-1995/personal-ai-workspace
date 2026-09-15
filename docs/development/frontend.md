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

## 一期工作流与概览

- 主导航集中为概览、项目、任务、笔记，项目与任务视图在各自页面内切换。
- 概览提供今日待办、逾期任务、进行中项目和笔记四项可点击指标；主体集中展示今日任务、项目、未来安排和最近笔记。宽屏分成两栏，窄屏顺序排列，内容最大宽度为 100rem。
- 概览中的任务标题可直接打开编辑；新建任务和新建项目通过路由搜索参数 `create` 打开表单。任务编辑通过 `taskId` 定位，项目分区通过 `tab` 保持上下文。
- 快捷面板提供实际创建入口和页面导航；顶部文案准确描述这一用途。
- 概览通过 `src/features/dashboard/api/dashboard.ts` 请求后端数据；任务操作调用任务 API，再通过公共查询失效入口刷新相关摘要。
- 创建项目后进入任务分区；空项目给出添加第一条任务的入口。项目归档后，任务操作和关联笔记编辑进入只读状态。

### 列表与返回上下文

- 项目列表通过 `page` 保存页码，项目详情通过 `from` 保存来源分区及页码，通过 `taskPage` / `notePage` 保留项目内列表页码。
- 笔记列表通过 `view`（收藏/归档）、`q`（关键词）、`page` 保存筛选；输入合并 300ms 后请求接口。
- 笔记详情使用 `from` 保留笔记列表上下文，使用 `project` 保留项目笔记分区、页码和项目列表来源。返回与删除后的跳转使用对应来源。
- 搜索参数由各业务 `lib/*-search.ts` 白名单校验；通用页码解析位于 `src/shared/lib/route-search.ts`。`ListPagination` 处理翻页和删除后页码超出范围的恢复入口。
- 项目与笔记列表、项目内任务和笔记均使用服务端分页，每页 20 条；项目内创建任务成功后回到第一页，按创建时间倒序展示。

本轮产品判断与改动范围见[一期交互复盘](../product/v0.1-ux-review.md)。

## 笔记与表单保存

- `src/features/note/hooks/use-note-draft.ts` 在停止输入 800ms 后保存，通过串行请求和版本号管理连续编辑；保存期间的新输入会接着保存。
- 笔记支持手动保存、Ctrl / Command + S、错误重试和复制当前内容。页面内导航先尝试保存，保存失败才提示继续编辑或放弃；刷新和关闭浏览器由离开保护提醒。
- 版本冲突保留当前输入，不自动覆盖其他位置的修改。草稿保留在当前页面内存中，不提供离线持久化。
- 项目和任务表单通过 `DraftDialog` 对取消、Escape、遮罩关闭、站内导航和浏览器返回提供放弃确认；刷新与关闭页面使用浏览器原生提醒。提交期间禁用输入，成功后放行关闭和跳转并清空表单。
- 删除操作等待服务端成功后退出，失败时继续保留操作上下文。

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

任务表关闭批量选择，复选框仅用于完成任务；状态可直接调整，标题与编辑按钮都能打开详情。状态与优先级直接使用后端枚举，保留“已取消”和“紧急”。全部任务列表可继续加载后续服务端分页，表格搜索针对已经加载的数据；项目内任务表关闭客户端分页，搜索与排序仅针对当前服务端页。

服务端数据仍由 TanStack Query 管理；仅侧栏、命令面板等客户端 UI 偏好进入 Zustand。
