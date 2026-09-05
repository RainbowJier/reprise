# 阶段二：任务拆分

## 流程

从已确认的设计文档和项目探测结果拆分独立、可验证、可追踪的任务。任务类型和文件位置必须由启用的 profile、相邻代码和真实依赖决定，不能因为模板存在就生成任务。

### 1. 前置读取

- 读取设计文档、现有迁移/Schema、项目规范和目标范围的相邻实现。
- 读取 `references/templates/task.md`；需要技术细节时读取 `references/code-conventions.md`。
- 复核结构分析记录：高风险影响面仍不确定时创建 `blocked` 任务，而不是猜测文件、接口或调用方。

### 2. 设计系统前置依赖

当阶段一检测到 Vue 前端缺少 `$FRONTEND_ROOT/design-system/`，且已按 `static/fragments/design-system-init.md` 生成并获得确认时：

- 将设计系统参考目录生成记录作为前置条件，不把它伪装成业务 UI 实现任务；
- 所有需要选择组件、Token 或 UI 交互的任务，必须依赖该前置条件完成后再执行；
- 初始化只产生 `$FRONTEND_ROOT/design-system/` 参考资料，不得把该目录列为运行时 import 来源；
- 如果目录冲突、前端根目录不明确或生成尚未确认，相关 UI 任务标记为 `blocked`，不得猜测组件或继续写 UI；
- 设计系统已存在时，将其作为只读输入，不重复创建或覆盖。

建议在任务摘要中明确记录：

```text
设计系统前置：<已存在并只读 / 已生成并确认 / blocked / 不适用>
参考目录：<$FRONTEND_ROOT/design-system/ 或“不适用”>
运行时引入：无
```

### 3. 通用任务类别

项目既有任务文档使用自有类型体系时，沿用项目类型填入 `type`，或用通用 `type` + `subtype` 字段承载项目类型；同一功能内保持一致，不混用两套体系。无既有体系时按实际需求从下表选择一个或多个类型：

| 类型 | 使用条件 | 典型内容 |
|------|----------|----------|
| `documentation` | 需要设计、运行说明或发布说明 | 设计、ADR、用户文档、变更日志 |
| `data_model` | 有模型或 Schema 设计 | 类型、实体、表、索引、关系、校验 |
| `migration` | 需要持久化变更 | 版本化迁移、回填、回滚、兼容策略 |
| `persistence` | 项目有数据访问层 | Repository/Mapper/ORM/查询优化 |
| `contract` | 有 API、事件、RPC 或类型契约 | 请求/响应、schema、错误、向后兼容 |
| `domain_logic` | 有领域或业务规则 | 校验、状态机、事务、策略、权限 |
| `service` | 有应用/服务层或用例 | 编排、命令/查询、异步任务 |
| `api` | 有服务端入口 | Controller/handler/resolver、认证、限流 |
| `ui` | 有前端或用户界面 | 页面、组件、路由/菜单权限入口、状态、可访问性 |
| `integration` | 有外部系统/SDK/消息 | client、重试、超时、幂等、契约核验 |
| `test` | 功能需要新增或调整验证 | 单元、集成、e2e、fixture、回归 |
| `observability` | 项目要求审计、日志、指标或告警 | trace、metric、audit、dashboard |

任务可以合并或细分，但每个任务必须有单一可验收目标。

### 3. 条件化架构映射

只有项目探测已经证实这些模式时才参考：

| 已确认模式 | 可选任务映射 |
|------------|--------------|
| Spring Boot MVC 分层 | entity/DTO、repository / mapper、service、controller 或项目既有目录任务 |
| Spring Boot DDD | 按项目实际 DDD 分层（如 domain/application/infrastructure/adapter）拆分模型、Gateway、DTO 映射、应用服务、Controller 任务 |
| Vue 前端 | API client 模块、页面视图（views）、组件、路由/菜单注册、Pinia/Vuex 状态、test 任务 |
| 数据库迁移工具 | Flyway/Liquibase 按既有 migration 版本、命名、up/down 或 rollback 约定拆分 |

### 4. 拆分规则

1. 按真实依赖安排任务；契约和 migration 可以先于实现；全栈功能先拆接口契约任务，前端实现依赖契约而非后端实现，可与后端并行，联调/回归任务依赖两侧实现；测试与文档按项目习惯并行或收尾。
2. 每项 API、UI 用户流、迁移或外部集成都应有独立任务，除非它们无法独立验证。
3. 任务文件名与任务 ID 遵循 SKILL.md「命名规则（唯一定义处）」：文件名 `{NN}_{type}_{short-slug}.md`，ID `{$TASK_PREFIX}_{NN}_{type}_{short_slug}`；序号为创建序号（首轮按依赖拓扑分配，新任务追加在末尾，不复用、不重排），执行顺序只由 `depends` 依赖图决定。
4. `files` 必须指向确认过的现有路径或明确新增路径；不列虚假的层和模块。
5. 每项任务写明验证命令或验证方式、风险和完成条件。
6. 涉及编码的任务必须包含「完整代码」章节（结构见 `references/templates/task.md`）：新增文件给全量代码，修改文件给修改后所在类/方法的完整代码块并注明精确落点，供用户手动敲写或直接落盘；代码依据已确认设计与相邻实现编写，禁止占位伪代码。依赖未完成或 `blocked` 无法给出完整代码时说明原因，条件具备后回填。执行中回写代码时在代码块末尾追加修订记录（`修订：N - 原因`），保留最终版；重大偏差同步更新设计文档。
7. 初始状态为 `pending`；缺依赖、用户决策、迁移依据或高风险影响分析时为 `blocked` 并填写原因。

## 输出

任务写入 `$DOC_PATH/tasks/`。按功能选择实际文件，示例：

```text
tasks/
├── 01_documentation_design.md
├── 02_contract_create-record.md
├── 03_migration_add-record-table.md
├── 04_service_create-record.md
├── 05_api_create-record.md
├── 06_ui_record-form.md
└── 07_test_create-record.md
```

## 生成校验

任务文件写入后逐项自检，不通过则修正后再进入确认关卡：

1. front matter 是合法 YAML：占位符已全部替换为实际值；含 `{}`、`|` 等特殊字符的值加引号。
2. `depends` 引用的任务 ID 均已存在，依赖图无环。
3. `files` 均为已确认存在的路径或明确新增路径。
4. 正文与代码块无残留 `{...}` / `{{...}}` 占位符和示例行。
5. 新任务编号追加在末尾；未修改历史任务的序号、状态与依赖。

## 确认关卡

```text
已生成 N 个任务：

| 编号 | 任务 ID | 类型 | 状态 | 依赖 / 阻塞原因 | 验收条件 |
|------|---------|------|------|-----------------|----------|
| 01 | feature_01_documentation_design | documentation | pending | — | 文档评审通过 |
| 03 | feature_03_migration_add_record | migration | blocked | 方言未确认 | migration 评审通过 |
| 07 | feature_07_test_create_record | test | pending | 04, 05 | 项目测试命令通过 |

请确认任务粒度、依赖关系、阻塞项和验证策略。
```

默认暂停等待用户确认后再进入开发。`$AUTO_MODE=true` 时不暂停，按核心立场「确认关卡与自动模式」自检放行：输出上述摘要后追加「自动放行的假设清单」，blocked 任务保持原状，其余任务进入阶段三执行。
