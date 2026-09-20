# Backend

Java 25 + Spring Boot 模块化单体应用。

## 启动

先按[项目启动说明](../../README.md)启动 PostgreSQL 和 Redis，再在本目录执行：

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS / Linux 使用 `./gradlew bootRun --args='--spring.profiles.active=local'`。

默认访问 [http://localhost:8080](http://localhost:8080)。

API 文档：[http://localhost:8080/scalar](http://localhost:8080/scalar)（OpenAPI：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)）。

## 文档

- [V0.2 实施状态、文件存储与隔离解析配置](../../docs/development/v0.2-execution.md)
- [后端开发指南与 API](../../docs/development/backend.md)
- [目录与扩展约定](../../docs/architecture/repository-layout.md)
- [V0.1 产品与技术规划](../../docs/product/v0.1-plan.md)
- [全部文档](../../docs/README.md)
