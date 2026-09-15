# 文档索引

[项目首页](../README.md) · [目录与扩展约定](architecture/repository-layout.md)

## 按用途查找

| 分类 | 文档 | 说明 |
| --- | --- | --- |
| 产品 | [V0.1 产品与技术规划](product/v0.1-plan.md) | 范围、模型、接口规划、迭代阶段；规划内容不代表全部已实现 |
| 产品 | [V0.1 交互复盘与改进](product/v0.1-ux-review.md) | 一期入口、下一步操作、保存反馈的复盘与本轮改动边界 |
| 架构 | [目录与扩展约定](architecture/repository-layout.md) | 当前目录、模块职责、新增应用与共享代码的放置规则 |
| 设计 | [UI 设计系统](design/ui-design-system.md) | 设计 Token、布局、主题、组件与交互规范 |
| 设计 | [工作台 UI 参考图](design/assets/workspace-ui-reference.png) | 设计参考素材 |
| 开发 | [前端开发指南](development/frontend.md) | 启动、认证交互、组件与布局说明 |
| 开发 | [后端开发指南](development/backend.md) | 启动、现有 API、认证、数据库与时间约定 |

## 文档存放规则

- `product/`：产品需求、版本规划；按版本命名，例如 `v0.1-plan.md`。
- `architecture/`：当前工程结构与技术约定。出现需要长期记录的架构决策时，再建立 `architecture/decisions/`，按 `0001-<topic>.md` 编号。
- `design/`：设计规范；参考图、流程图等文档素材放在其 `assets/` 目录，采用能说明用途的名称。
- `development/`：安装、启动、配置、调试和应用开发说明。后续有独立部署流程时再新增 `operations/`。
- 应用根目录的 `README.md` 只保留应用介绍、最短启动步骤和文档链接；详细说明在这里维护一份。
- 文档统一使用小写英文文件名和连字符；正文可使用中文；仓库内链接使用相对路径。
- 新增或移动文档时，同时更新本索引和相关入口链接。

目录规范以[目录与扩展约定](architecture/repository-layout.md)为准；现有行为以代码和开发指南为准；版本规划用于描述目标范围。
