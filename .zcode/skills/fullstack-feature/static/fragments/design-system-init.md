# 前置阶段：初始化 design-system 参考目录

本片段仅在已启用 `frontend` profile，且目标前端根目录下不存在完整 `design-system/` 时加载。

## 目标

在 `$FRONTEND_ROOT/design-system/` 生成一套**仅供 AI agent / 开发者参考**的设计系统资料。该目录不是运行时依赖：

- 不修改现有业务代码；
- 不在前端代码中新增指向 `design-system/` 的 `import` 或 `@import`；
- 不增加 npm 依赖、别名、lint 插件或 `scripts/`；
- 不覆盖目标目录中已有文件；
- 不把参考实现当作目标项目的组件库直接引用。

## 前置探测

在生成前记录以下事实：

| 项目 | 要读取的事实 |
|------|--------------|
| 前端根目录 | 用户指定路径优先；否则由 `package.json`、Vue 依赖、构建配置和 `src/` 交叉确认 |
| 技术栈 | Vue 版本、Vite/Webpack/Vue CLI、JavaScript/TypeScript、包管理器 |
| 现有组件 | `src/components/`、已有 UI 目录、组件库依赖、相邻页面用法 |
| 现有样式 | 全局 CSS/SCSS、CSS Modules、主题文件、CSS 自定义属性、Tailwind/UnoCSS 配置 |
| 现有文档 | README、AGENTS.md、贡献指南、已有组件规范 |
| 验证方式 | package.json 中已有的 lint、typecheck、test、build 命令；不存在则记为 unknown |

## 目录状态判断

按以下顺序处理：

1. `$FRONTEND_ROOT/design-system/` 不存在：进入生成流程；
2. 目录存在且包含 `README.md`、`design-system.md`、`rules.md`、`tokens.md`：视为已有参考目录，只读，不重新生成；
3. 目录存在但缺少上述文件，或包含非本 skill 生成的用户内容：标记 `blocked`，列出冲突文件，等待用户确认，不删除、不覆盖；
4. `$FRONTEND_ROOT` 无法唯一确认，或发现多个前端候选：标记 `blocked`，先请求选择目标，不猜路径。

## 默认确认关卡

在阶段一确认摘要中追加：

```text
设计系统参考目录：<已存在并只读 / 将生成 / blocked>
生成位置：<$FRONTEND_ROOT/design-system/ 或“不适用”>
参考实现来源：<已有项目组件 / 外部 UI 库说明 / 无，只有文档骨架>
是否允许生成：<是 / 待确认>
运行时引入：明确为“无”
```

未处于 `$AUTO_MODE` 时，得到用户确认后再生成。`$AUTO_MODE=true` 仅可在目标路径明确、目录不存在、目标可写且不会覆盖文件时自检放行；任何冲突保持 `blocked`。

## 生成规则

从 `references/templates/design-system/` 读取四个文档模板和 `ui/README.md` 模板，在 `$FRONTEND_ROOT/design-system/` 创建：

```text
$FRONTEND_ROOT/design-system/
├── README.md
├── design-system.md
├── rules.md
├── tokens.md
└── ui/
    └── README.md
```

### README.md

说明：

- 该目录是 AI agent / 开发者参考资料；
- 不属于运行时依赖，不允许项目代码 import；
- `ui/` 是参考实现快照或参考说明，不是当前项目的直接依赖；
- 资料与目标项目自有代码的关系是 copy & own；
- 生成日期不作为同步依据，避免跨天误报。

### design-system.md

根据已读取的实际项目生成，不得虚构：

- 如果项目已有自有 UI 组件，逐个登记真实组件名称、源码路径、用途、Props、Emits、Slots、使用时机、禁用场景和相似组件辨析；
- 如果项目使用外部组件库，只登记项目实际使用的封装组件或约定，不复制第三方源码；
- 如果没有可确认的组件，保留“组件清单为空，后续按项目事实补充”的说明和固定条目格式；
- `ui/` 参考快照中出现的组件必须能在来源项目中找到，不允许用占位组件冒充已有实现。

### rules.md

至少包含以下硬约束：

- `design-system/` 只读参考，运行时代码不得 import；
- 新功能优先复用目标项目已有组件；
- 新建组件前先检查已有组件和项目规范；
- 颜色、间距、字号、圆角、阴影等遵循项目已存在的 token 体系；
- 没有 token 体系时标记为未识别，不擅自重构全局样式；
- 新增或修改组件时同步更新参考文档。

### tokens.md

根据实际文件整理人类可读 Token 词汇表：

- 优先读取已有 CSS 变量、主题文件、SCSS 变量、Tailwind/UnoCSS 配置；
- 分为颜色、字体、间距、圆角、阴影、层级、动效等类别；
- 每个条目标注“已有事实”或“建议 token”；
- 没有确认值的类别写“未识别”，不要猜颜色或数值；
- 如果源文件包含字面值，文档可以记录它，但不要为了生成文档改动源文件。

### ui/README.md

说明 `ui/` 目录是参考实现区：

- 当前项目代码不得从 `$FRONTEND_ROOT/design-system/ui/` 引入；
- 已有组件可复制到目标项目自己的组件目录后再适配；
- 外部 UI 库只记录使用方式，不复制依赖源码；
- 没有组件时保持空目录说明，不批量创建组件。

## 生成后自检

生成完成后重新读取新目录并确认：

1. 四个文档和 `ui/README.md` 均存在；
2. 文档中的项目名称、技术栈、源码路径和组件数量来自实际读取结果；
3. 没有残留 `{...}`、`{{...}}`、`<PROJECT_...>` 等模板占位符；
4. 目标前端代码没有新增 `design-system/` import 或 `@import`；
5. 没有覆盖已有文件，也没有修改 `package.json`、构建配置和业务代码；
6. 生成动作和限制写入阶段一结构分析记录；
7. 生成成功后，阶段一继续读取 `design-system.md` 与 `rules.md`，作为后续设计约束。

## 输出格式

```text
已初始化 design-system 参考目录：
- 前端根目录：<路径>
- 输出目录：<路径>/design-system/
- 组件来源：<已有项目组件 / 外部 UI 库说明 / 无>
- 识别组件：<数量>
- 识别 Token 分类：<列表>
- 运行时引入：无
- 未修改：<业务代码 / package.json / 构建配置 / 依赖>
- 阻塞项：<无 / 冲突与解除条件>
```
