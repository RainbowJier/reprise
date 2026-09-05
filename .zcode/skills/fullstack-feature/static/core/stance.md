# 开发立场与全局规则（Vue + Spring Boot 全栈）

始终加载。本文件定义本 skill 面向 Vue + Spring Boot 全栈开发的立场与栈默认约定；项目自身规范和已验证代码模式优先于本文件的默认建议。

## 规范优先级与项目探测

按以下优先级解释规则：

1. 用户本次明确要求。
2. 目标项目的 `AGENTS.md`（含仓库根与目标模块级）、`CONTRIBUTING.md`、README、架构文档、构建清单和 CI 配置；模块级规范优先于根规范的通用描述。
3. 目标模块相邻代码、测试和迁移文件中的实际模式。
4. 本 skill 的通用默认值。

`references/profiles/` 下的项目 profile 文件是第 2 级（项目文档）的索引与术语翻译层，不是独立规范来源；与项目文档冲突时以项目文档原文为准。仅当项目探测证实目标项目与 profile 匹配时才加载。

每个阶段开始前记录项目探测结果：

| 类别 | 必须确认的事实 |
|------|----------------|
| 范围 | 仓库根目录、目标模块、相关入口和输出目录 |
| 技术栈 | 后端：Java 版本、Spring Boot 版本、Maven/Gradle、多模块结构；前端：Vue 版本、构建工具、UI 组件库、状态管理、路由与菜单来源；包管理器和代码生成工具 |
| 架构 | 分层/DDD/MVC/六边形/模块化边界、依赖方向和公共库 |
| 数据 | 数据库、Schema、迁移工具、模型映射、索引、事务和回滚方式 |
| 接口 | REST/GraphQL/RPC/事件、认证、错误模型和兼容策略 |
| 集成 | HTTP client（RestTemplate/WebClient/OpenFeign）、SDK、消息队列、AI、文件或其他外部依赖 |
| 验证 | lint、format、unit、integration、e2e、build、package、deploy-check 命令 |

未知事实必须标记为 `unknown`，不得用本仓库或训练数据的惯例猜测。

## 工具与 CodeGraph 降级

如果项目提供 CodeGraph 或其他结构索引，优先用于符号、调用链和影响面分析；常见查询顺序为 `context → search → callers/callees/trace → impact`。如果没有该工具，不把它当作通用硬前提。

工具不可用、索引过期或查询失败时：

1. 记录工具状态和未确认的结论。
2. 使用可用的只读文件、目录和文本检索进行降级分析，并明确标注不是结构索引结论。
3. 对跨模块/跨服务契约、权限、数据迁移、并发、删除或其他高风险变更，无法可靠确认影响面时只生成设计和 `blocked` 任务，等待确认；不得自动修改业务代码。
4. 不得声称执行过不存在的工具。验证同步命令仅在项目确实提供时执行。

## 通用设计原则

- 先复用相邻实现和公共能力，再引入新抽象。
- 先写设计、影响范围和任务，再改代码；扫描已有产物，避免覆盖用户修改。
- API、UI、后台任务、事件和数据模型只在需求实际涉及时生成。
- 全栈功能先冻结接口契约（设计文档「六、接口设计」或项目 OpenAPI 定义为唯一事实），前后端基于同一契约并行开发，字段命名与错误模型保持一致；联调是独立验证步骤，不并入任一侧。
- 明确权限、审计、可观测性、幂等、错误处理、兼容性、性能和回滚策略；不适用时写明原因。
- 跨模块/服务契约必须列出所有调用方和兼容方案。远程失败要保留上下文并按项目错误处理约定传播；事务边界由项目现有模式决定。
- 不在未知数据库方言或未知迁移机制下生成可执行 DDL；先读取真实项目配置和迁移文件。

## Profile 规则

### 架构 profile

仅当项目已存在对应架构时复用它。后端检测 Spring Boot 单体或多模块 Maven/Gradle reactor 的分层（MVC 三层、DDD、六边形或模块化边界），前端检测 Vue 工程组织方式（按页面、feature 或组件域）；保持既有依赖方向，按相邻实现落位。不要把 Gateway、Entity、Mapper 等特定名称泛化为必需层。

### Spring Boot 后端 profile

检测到 Java + Maven/Gradle + Spring Boot（`@SpringBootApplication`、spring-boot-starter-*）后启用。构造注入、`@Transactional` 事务边界、参数校验、DTO 映射（MapStruct/BeanUtil/手写，以项目为准）、统一响应与异常处理遵循项目已有模式；数据访问按项目实际选型（Spring Data JPA、MyBatis/MyBatis-Plus、jOOQ 等），同一数据域内不混用两套。依赖版本由项目 BOM/parent 管理，不在 feature 中猜测或升级。具体包路径、返回包装和错误工具以源码为准。

### Vue 前端 profile

检测到 Vue 工程（package.json 含 vue；Vue 2/3、Vite/Webpack/Vue CLI、vue-router、Pinia/Vuex、Element Plus/Ant Design Vue/Naive UI 等）后启用。组件风格（`<script setup>` 组合式或选项式）、API client 封装（axios 实例、拦截器、错误提示）、状态管理与 UI 组件复用遵循相邻实现。新页面按工程既有方式注册路由——静态路由或后端菜单/权限数据动态下发；入口由后端数据驱动时，前端页面任务与后端菜单/权限数据任务必须成对拆分——没有注册入口的页面不可达。验证以工程实际脚本为准（lint/typecheck/test/build）；无 lint/测试配置时退化为 build + 手动验收步骤，如实记录，不得虚构检查命令。

### 数据库 profile

仅在功能涉及持久化时启用。先确认数据库方言、ORM 映射与迁移工具（Flyway/Liquibase 或项目自有策略）；区分新建 schema、版本化 migration、字段变更和数据回填。DDL 与 JPA 实体/MyBatis mapper 变更同步落位。新变更创建新的增量任务，不能回写已完成任务的历史范围。

### API/集成 profile

按项目现有契约（REST 为主，GraphQL/RPC/事件按实际）、认证方式（Spring Security/JWT/token 头等以项目为准）、HTTP client（RestTemplate/WebClient/OpenFeign）、消息协议和测试方式实现；不得凭空添加另一种框架或协议。受控认证头与上下文只能按项目已有安全边界传播。

## 确认关卡与自动模式

阶段一（设计）、阶段二（任务拆分）结束后默认暂停等待用户确认。`$AUTO_MODE=true`（触发命令含 `--auto` 或 `-y`）时改为自检放行：

1. 仍输出同格式的关卡摘要，保持可审计。
2. 在摘要后追加「自动放行的假设清单」，逐条列出本次未经用户确认的决策。
3. 以下高风险项**不自动放行**，保持 `blocked` 并填写原因，但不阻塞其余任务的拆分与执行：
   - 数据迁移、数据回填或删除类变更；
   - 权限、认证或跨服务/跨模块契约变更；
   - `$DOC_PATH` 无项目惯例可依时的输出目录选择；
   - 影响面在降级分析下仍无法可靠确认的改动。
4. 阶段三执行到 `blocked` 任务时跳过，在完成报告中单独汇总，等用户决策后处理。

自动放行只减少等待，不降低验证标准；验证失败或未执行仍不得标记 `completed`。

## 任务状态与验证

状态定义：

- `pending`：尚未开始。
- `in_progress`：正在修改，尚未完成验证。
- `completed`：实现、文档和项目要求的验证均成功。终态，不得回退或改写历史任务范围；后续增量变更走 field-add 新任务。
- `blocked`：依赖、结构、文档、环境或用户决策阻塞，必须填写 `blocked_reason`。

状态迁移（只允许下表路径）：

| 当前状态 | 事件 | 目标状态 | 守卫条件 |
|----------|------|----------|----------|
| pending | 开始执行 | in_progress | `depends` 中全部任务为 completed |
| in_progress | 实现 + 必需文档 + 验证全部成功 | completed | 验证命令实际执行且通过 |
| in_progress | 缺依赖/结构/文档/环境/权限/用户决策 | blocked | `blocked_reason` 含缺口类别、描述、解除条件 |
| in_progress | 验证失败且自动修复超三轮或属高风险问题 | blocked | `blocked_reason` 记录失败命令与原因 |
| blocked | 用户补充决策或信息，阻塞原因消除 | pending | 见下方解除协议 |

`blocked` 解除协议：

1. 将 `blocked_reason` 清空，状态改回 `pending`。
2. 在任务「风险与阻塞」章节追加解除记录：`[unblock] 日期：决策人=用户，决策=<内容>`。
3. 重新校验 `depends`；依赖仍不满足时保持 `pending`，不抢跑。
4. 历史已完成任务不得因后续变更回退或改写——增量变更一律走 field-add 新任务。

验证命令必须从项目构建清单、CI 或相邻模块识别。每次改动后执行最小相关验证，再执行项目要求的完整验证；自动修复最多三轮。验证失败或未执行不得标记 `completed`，报告必须列出实际命令、结果和跳过原因。
