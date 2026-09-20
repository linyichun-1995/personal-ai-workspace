# 文档索引

[项目首页](../README.md) · [目录与扩展约定](architecture/repository-layout.md)

## 按用途查找

| 分类 | 文档 | 说明 |
| --- | --- | --- |
| 产品 | [V0.1 产品与技术规划](product/v0.1-plan.md) | 范围、模型、接口规划、迭代阶段；规划内容不代表全部已实现 |
| 产品 | [V0.1 交互复盘与改进](product/v0.1-ux-review.md) | 一期入口、下一步操作、保存反馈的复盘与本轮改动边界 |
| 产品 | [V0.2 二期产品规划](product/v0.2-plan.md) | 二期总入口、一期实际基线、范围、优先级与成功标准 |
| 产品 | [V0.2 产品需求与业务规则](product/v0.2-requirements.md) | 文件、附件、标签、搜索、生命周期与 AC01 至 AC16 验收规则 |
| 产品 | [V0.2 二期任务清单](product/v0.2-backlog.md) | 27 项 P0 任务，含 RustFS S3 封装与兼容验收、依赖及工作量 |
| 产品 | [V0.2 二期实施计划](product/v0.2-delivery-plan.md) | 11 周排期、角色容量、里程碑、风险与范围控制 |
| 产品 | [V0.2 业界方案调研](product/v0.2-research.md) | 官方来源、产品参考、方案比较与采用边界 |
| 架构 | [目录与扩展约定](architecture/repository-layout.md) | 当前目录、模块职责、新增应用与共享代码的放置规则 |
| 架构 | [V0.2 技术架构](architecture/v0.2-architecture.md) | 文件处理、持久化任务、中文关键词检索与一致性设计 |
| 架构 | [V0.2 数据模型与接口](architecture/v0.2-data-api.md) | 表结构、约束、API、并发、错误码和迁移约定 |
| 架构 | [V0.2 RustFS 与 S3 API 二次封装](architecture/v0.2-s3-storage.md) | 项目统一存储 API、AWS SDK v2 适配、字节校验、流管理和错误模型 |
| 设计 | [UI 设计系统](design/ui-design-system.md) | 设计 Token、布局、主题、组件与交互规范 |
| 设计 | [V0.2 页面与交互设计](design/v0.2-interaction.md) | 文件和搜索页面、附件、标签、路由与异常反馈 |
| 设计 | [工作台 UI 参考图](design/assets/workspace-ui-reference.png) | 设计参考素材 |
| 开发 | [前端开发指南](development/frontend.md) | 启动、认证交互、组件与布局说明 |
| 开发 | [后端开发指南](development/backend.md) | 启动、现有 API、认证、数据库与时间约定；交互式文档见 Scalar |
| 开发 | [V0.2 测试验收与发布计划](development/v0.2-quality-release.md) | 功能与安全用例、性能目标、发布门槛、灰度和恢复演练 |
| 开发 | [V0.2 RustFS 现有环境与接入配置](development/v0.2-rustfs-integration.md) | 用户已运行容器、连接地址、私有桶、权限、配置和维护 |
| 开发 | [V0.2 RustFS 与 S3 兼容验收](development/v0.2-s3-acceptance.md) | 17 项兼容用例、实际镜像与 SDK 组合、故障和验收记录 |

## 文档存放规则

- `product/`：产品需求、版本规划；按版本命名，例如 `v0.1-plan.md`。
- `architecture/`：当前工程结构与技术约定。出现需要长期记录的架构决策时，再建立 `architecture/decisions/`，按 `0001-<topic>.md` 编号。
- `design/`：设计规范；参考图、流程图等文档素材放在其 `assets/` 目录，采用能说明用途的名称。
- `development/`：安装、启动、配置、调试和应用开发说明。后续有独立部署流程时再新增 `operations/`。
- 应用根目录的 `README.md` 只保留应用介绍、最短启动步骤和文档链接；详细说明在这里维护一份。
- 文档统一使用小写英文文件名和连字符；正文可使用中文；仓库内链接使用相对路径。
- 新增或移动文档时，同时更新本索引和相关入口链接。

目录规范以[目录与扩展约定](architecture/repository-layout.md)为准；现有行为以代码和开发指南为准；版本规划用于描述目标范围。
