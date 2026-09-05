---
name: fullstack-feature
description: >-
  Vue + Spring Boot 全栈新功能设计与开发工作流。Use when the user asks for a new feature,
  requirements design, or full-stack feature development, or says "新功能"、"需求设计"、"功能开发".
  先探测项目规范、架构、工具链和影响范围，再经过需求评审、任务拆分、增量实现与验证。
  默认按 Spring Boot 后端（Maven/Gradle、JPA/MyBatis）+ Vue 前端（Vite/Webpack、vue-router、
  Pinia/Vuex、组件库）探测与落位；项目规范与相邻实现优先于本 skill 的栈默认约定，
  纯后端/纯前端仓库自动裁剪另一侧任务。项目术语映射 profile 为可选扩展
  （references/profiles/TEMPLATE.md），由项目探测条件加载。
version: 5.2.0
---

# Vue + Spring Boot 全栈新功能工作流 — 路由器

本 skill 面向 Vue + Spring Boot 全栈项目：默认按该栈探测与落位，但项目自身规范和相邻实现始终优先。先加载项目事实，再按需加载阶段片段和 profile 参考。

## 路由协议

### 1. 加载 manifest 与核心立场

读取 [manifest.yaml](manifest.yaml) 和 `static/core/stance.md`。核心立场规定项目探测、规范优先级、工具降级、确认关卡、任务状态和验证闭环。

### 2. 解析输入

| 参数 | 来源 | 说明 |
|------|------|------|
| `$ARGUMENTS` | 触发命令 | 需求描述文本 |
| `$DOC_PATH` | 触发命令或用户确认 | 设计文档输出目录；未提供时根据项目习惯提议，不擅自选择已有目录 |
| `$DISPLAY_NAME` | 需求描述 | 功能的业务显示名称 |
| `$FILE_SLUG` | 从显示名称生成并确认 | 安全的 lowercase kebab-case 文件名，不含路径分隔符 |
| `$TASK_PREFIX` | 从 slug 生成 | 稳定的 ASCII 任务 ID 前缀 |
| `$AUTO_MODE` | 从 `$ARGUMENTS` 解析 `--auto` 或 `-y` | 缺省 `false`。`true` 时阶段一、二确认关卡改为自检放行，规则见 `static/core/stance.md`「确认关卡与自动模式」 |
| `$FRONTEND_ROOT` | 项目探测或用户确认 | 目标前端工程根目录；design-system 缺失时固定生成到 `$FRONTEND_ROOT/design-system/`，不放仓库根目录或 `src/` |

如果目标范围、输出位置、功能边界或高风险约束不明确，先询问用户；不要因缺少技术信息而套用默认架构。`$AUTO_MODE=true` 只改变确认关卡行为，不豁免高风险项——它们保持 `blocked`（见核心立场）。

### 无参数触发时的交互式入口

触发时未携带需求描述（只有触发词或空参数）时，不直接进入阶段一；先以提问形式输出场景菜单让用户选择：

| 选项 | 场景 | 进入流程 |
|------|------|----------|
| 1 | 新增功能：全新的业务功能 | 阶段一（需求分析与设计） |
| 2 | 继续开发：恢复上次未完成的功能 | 读取已有 `tasks/` 恢复各任务状态，从阶段三断点继续 |
| 3 | 加/改字段：向已有模型/表/schema/接口类型增改字段 | field-add 特殊流程 |
| 4 | 修改行为：已有功能的非字段类变更 | 读取已有设计与任务，作为新增量任务追加执行 |
| 5 | 修复缺陷：功能不符合预期 | 定位问题直接修复，修复与验证记录回写对应任务 |

用户选定场景后，按场景追问**最少必要信息**（触发消息中已提供的不重复追问）：

- 场景 1：功能需求描述；`$DOC_PATH` 未定时按项目惯例提议。
- 场景 2：设计文档与 `tasks/` 目录；存在 `blocked` 任务时顺带收集决策，随后按核心立场「blocked 解除协议」处理。
- 场景 3：目标对象与字段明细（字段名、类型、可空/默认值、语义）；产物目录。
- 场景 4：现状与目标的差异描述；产物目录。
- 场景 5：现象、复现步骤与期望行为；任务目录。

场景 2–5 用户给不出产物路径时，先按命名规则与 `tasks/` 目录约定在仓库内探测，探测不到再询问。场景判断有歧义时（如「改一下导出」既可能是加字段也可能是改行为），用菜单向用户确认而不是自行猜测。交互式入口只改变信息收集方式，不改变确认关卡、自动模式与高风险规则；带完整需求描述触发时跳过菜单直接进入对应流程。

### 命名规则（唯一定义处，其余文件引用此处）

占位符语法（全 skill 统一）：`$UPPER_CASE` 为运行时变量（由触发命令或用户确认得到）；`{lower_case}` 为模板填充槽（生成文件时整体替换，替换后的成品不得残留任何占位符）。

1. `$DISPLAY_NAME`：业务显示名，可含中文。
2. `$FILE_SLUG`：由显示名生成的 lowercase kebab-case，仅 `[a-z0-9-]`，不含路径分隔符，需用户确认。
3. `$TASK_PREFIX` = `$FILE_SLUG`。
4. 任务文件名 = `{NN}_{type}_{short-slug}.md`：`NN` 为两位创建序号（首轮按依赖拓扑分配，后续新任务一律追加递增，不复用、不重排）；`type` 为任务类型（见任务拆分片段；项目既有任务文档的自有类型体系优先）；`short-slug` 为 kebab-case。
5. 任务 ID = `{$TASK_PREFIX}_{NN}_{type}_{short_slug}`：即文件名去扩展名、加前缀，连字符替换为下划线。
6. 序号只代表创建顺序；执行顺序仅由任务 `depends` 依赖图决定。禁止给已完成任务追加依赖或改写其序号。
7. 示例：显示名“数据导出” → slug `data-export` → 文件 `03_migration_add-record.md` → ID `data-export_03_migration_add_record`。

**通用产物约定**：

| 路径 | 内容 |
|------|------|
| `$DOC_PATH/$FILE_SLUG.html` | 可选的单文件设计文档；纯文档项目可改为 Markdown |
| `$DOC_PATH/tasks/*.md` | 按依赖排序的任务文件 |
| `$DOC_PATH/schema/` 或项目实际数据变更策略的落位目录 | 仅在确认数据库与数据变更策略后生成对应脚本；策略未确认时不生成 |

### 3. 探测项目并选择 profile

先读取项目规范、README、构建清单（`pom.xml`/`build.gradle`、`package.json`、vite/vue 配置）、CI 配置和相邻实现，用项目事实确认适用 profile：

- `architecture`：Spring Boot 单体或多模块 Maven/Gradle reactor 的分层（MVC 三层、DDD、六边形）与前端工程结构（按页面或 feature 组织）。
- `backend`：Spring Boot——Java 版本、Maven/Gradle、实际使用的 starter（Web/Data/Security/Validation 等）与数据访问选型（JPA/MyBatis/jOOQ）。
- `frontend`：Vue 2/3、构建工具（Vite/Webpack/Vue CLI）、vue-router、状态管理（Pinia/Vuex）、UI 组件库与 API client 封装。
- `database`：数据库类型、ORM 映射、迁移工具（Flyway/Liquibase 或项目自有策略）；未知方言时不生成可执行 DDL。
- `api`：REST（Spring Controller 契约）为主，项目存在 GraphQL/RPC/事件时同样探测。
- `integration`：HTTP client（RestTemplate/WebClient/OpenFeign）、消息队列、第三方 SDK、AI/数据处理等外部依赖。
- `testing`：后端 `mvn/gradle` 测试、前端 npm 脚本（lint/typecheck/test/build）、e2e 与 CI 命令。

只启用已由项目事实证实的 profile；项目规范与相邻实现优先于本 skill 的栈默认约定。纯后端或纯前端仓库自动裁剪另一侧任务；没有数据库或远程依赖时不创建对应任务。全栈功能遵循契约先行：接口契约冻结后前后端任务并行，联调作为独立验证任务依赖两侧（见核心立场）。

### 设计系统参考目录前置处理

启用 `frontend` profile 后，必须确定 `$FRONTEND_ROOT`，并检查 `$FRONTEND_ROOT/design-system/`：

- 已存在完整目录：只读 `README.md`、`design-system.md`、`rules.md`、`tokens.md` 与 `ui/`，将其作为本项目的 AI agent 参考资料，不覆盖、不引入；
- 完全不存在：加载 `static/fragments/design-system-init.md`，在阶段一确认后按固定模板生成 `$FRONTEND_ROOT/design-system/`；`--auto` 仅在前端根目录明确、目标目录不存在且没有覆盖风险时自检放行；
- 只存在部分文件、目录中有用户内容或前端根目录不明确：保持 `blocked`，先报告冲突，不删除、不覆盖、不猜路径；
- 生成的 `design-system/` 只供 agent / 开发者阅读，前端运行时代码不得从该目录 `import` 或 `@import`。生成后重新读取文档，再继续阶段一设计。

### 4. 按阶段加载片段

| 阶段 | 文件 | 需要确认 |
|------|------|----------|
| 前置：设计系统参考目录（仅检测到 frontend profile 时） | `static/fragments/design-system-init.md` | 条件确认 |
| 一：需求分析与设计 | `static/fragments/analysis.md` | 是 |
| 二：任务拆分 | `static/fragments/task-decomposition.md` | 是 |
| 三：增量开发与验证 | `static/fragments/development.md` | 否 |
| 特殊场景：已有字段/Schema 变更（触发条件、切入与返回路径见 manifest `special_flows` 与片段开头） | `static/fragments/field-add.md` | 条件确认 |

阶段一、二结束后默认暂停等待用户确认；`$AUTO_MODE=true` 时按核心立场「确认关卡与自动模式」自检放行。缺少项目规范、关键调用链、迁移依据或验证命令时，记录限制并将受影响任务标记为 `blocked`；不得虚报完成。

### 5. 按需加载参考

| 场景 | 文件 |
|------|------|
| 项目探测命中部署处附带的项目 profile（`references/profiles/` 下的项目文件） | `references/profiles/<project>.md` |
| 为目标项目编写新的术语映射 profile | `references/profiles/TEMPLATE.md` |
| 编写设计文档 | `references/templates/design-doc.html` |
| 数据库或 Schema 变更 | `references/templates/sql.md` |
| 拆分任务 | `references/templates/task.md` |
| 需要 Vue / Spring Boot 实现示例 | `references/code-conventions.md` |
| 前端缺少 `design-system/` 参考目录 | `static/fragments/design-system-init.md` 与 `references/templates/design-system/` |

参考文件中的具体技术示例只有在对应 profile 已确认后才可使用。项目 profile 是项目文档的索引与翻译层，与项目文档冲突时以后者为准。
