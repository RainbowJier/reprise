# Vue 与 Spring Boot 架构与实现模式速查

本文件提供条件化参考，不替代目标项目的规范。先读取项目文档、构建配置和相邻代码，确认 profile 后再选择相应模式；项目已有写法与本文件冲突时以项目为准，不要把示例写法当作通用要求。

## 通用实施顺序

1. 确认目标架构边界、命名、错误处理、认证、日志和测试习惯。
2. 在现有结构中定位功能：UI、契约、业务逻辑、持久化、集成、异步、迁移和测试按需选择。
3. 复用相邻类型和公共工具；只有缺少合适能力时才引入抽象、依赖或新目录。
4. 对 API、事件、Schema 和跨模块调用保持兼容性，并用项目现有测试方式验证。

## 条件 profile 示例

### Spring Boot 分层/MVC（项目已有该结构时）

```text
Controller（REST 入口、参数校验） → Service（业务规则、事务边界） → Repository/Mapper（数据访问） → 数据库
```

```java
@Service
@RequiredArgsConstructor
public class ExampleService {
    private final ExampleRepository exampleRepository;

    @Transactional
    public ExampleResponse create(CreateExampleRequest request) {
        // 使用项目已存在的校验、映射、异常和响应模型
        return null;
    }
}
```

- 构造注入、`@Transactional`、参数校验注解、DTO 映射（MapStruct/BeanUtil/手写）遵循项目已有用法。
- 统一响应与全局异常处理复用项目既有组件，不自造第二套。
- 数据访问选型（Spring Data JPA / MyBatis / MyBatis-Plus / jOOQ）以项目为准，同一数据域内不混用两套。
- Maven/Gradle 依赖版本由项目 BOM、catalog 或 parent 管理，不在 feature 中猜测版本。

### DDD 或分层后端（项目已有该边界时）

```text
transport/adapter → application/use case → domain abstraction → infrastructure adapter
```

- 领域模型、Repository/Gateway、ORM/Mapper、Service/Use Case、Controller 的职责以项目代码为准。
- application/service 不应绕过已存在的基础设施抽象直接调用外部 client；远程失败、日志和结果转换落在项目既有 adapter/client 边界。
- 事务边界、DTO 映射与异常类型遵循现有实现，而不是固定框架名称。

### Vue 前端 feature

```text
路由/页面（views） → 组合式函数或状态（Pinia/Vuex） → API client 模块（axios 封装） → 复用组件 → 单元/e2e 测试
```

- **设计系统参考目录（本项目强制）**：检测到 Vue 前端后，先确定 `$FRONTEND_ROOT` 并检查 `$FRONTEND_ROOT/design-system/`。目录已存在时只读 `README.md`、`design-system.md`、`rules.md`、`tokens.md` 与 `ui/`；目录缺失时按 `static/fragments/design-system-init.md` 使用 `references/templates/design-system/` 生成到前端根目录。`design-system/` 只供 AI agent / 开发者参考，任何前端运行时代码不得从它 `import` 或 `@import`，不增加依赖、别名或脚本。
- **存量项目接入设计系统（copy & own）**：目标项目没有 `design-system/` 时，先完成设计系统参考目录的条件前置流程，再由 agent 根据参考资料把需要的实现复制/适配到目标项目自己的组件目录；不跨仓库引用参考目录，不覆盖已有文件。
- 组件风格（`<script setup>` 组合式或选项式）、命名、样式方案（scoped SCSS / CSS Modules / tailwind 等）跟随相邻实现。
- API 请求统一走项目既有 axios 实例与拦截器：认证头、错误提示、防重复提交等不自造第二套。
- 组件需要覆盖 loading、empty、error、permission 和响应式状态，范围以需求为准。
- 全栈功能先冻结 API 契约：前端 API client 与后端 Controller 同源对齐（字段命名、错误模型、认证头一致）；契约冻结后前后端并行开发，联调任务依赖两侧实现。
- 路由、菜单或权限入口由后端数据驱动时，页面任务必须与后端菜单/权限数据任务成对出现；页面没有注册入口等于不可达。
- 无 lint/测试配置的 Vue 工程，验证退化为 build + 手动验收步骤，如实记录，不得虚构检查命令。
- 设计系统参考目录生成本身不等于业务实现完成；生成后重新读取文档，再按目标项目现有组件、样式和验证方式开发。

### API 与集成

- 先确认 REST、GraphQL、RPC、消息或 SDK 契约；记录版本、认证、超时、重试、幂等、错误模型和调用方影响。
- HTTP client（RestTemplate/WebClient/OpenFeign）与认证方式（Spring Security/JWT/token 头等）按项目已有用法；受控认证/上下文 header、token、cookie 和密钥只能按项目已有安全边界签发、清洗和传播，不能在业务代码中伪造。
- 新增或修改公共契约前，列出调用方、兼容期和测试策略。

### 数据与 Schema

- 先确认真实存储、方言、ORM/查询层和 migration 工具，再生成变更。
- JPA 实体或 MyBatis mapper 与 DDL 变更必须同步落在同一任务或成对任务中。
- 新表、增量字段、索引、约束、回填和数据修复必须分清，采用项目已有版本和回滚约定。
- 无持久化需求时不创建模型、数据库脚本或 repository 任务。

## 常见质量要求

| 领域 | 需要确认的事项 |
|------|----------------|
| 权限 | 认证、授权、数据范围、租户隔离、审计 |
| 可靠性 | 超时、重试、幂等、降级、事务和失败恢复 |
| 性能 | 数据量、分页、缓存、索引、并发与资源上限 |
| 可观测性 | 日志、trace、metric、告警和敏感数据脱敏 |
| 兼容性 | API/schema/event 版本、发布顺序、回滚与灰度 |
| 测试 | 现有 lint、format、unit、integration、e2e、build/CI 命令 |
