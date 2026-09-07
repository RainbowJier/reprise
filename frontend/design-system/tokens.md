# Token 清单（命名词汇表）

> 本文件由 fullstack-feature 根据目标项目现有样式事实整理，供 AI agent / 开发者参考。
> 它不是运行时配置；没有确认值的内容必须标记为"未识别"或"建议"。

## 项目事实

- 前端根目录：`frontend/`
- Token 体系：**已决策采用 Tailwind CSS v4**（`@theme` CSS-first 配置 + utility-first，用户指定）；接入落地于场景 01 前端任务，实施前 `style.css` 仍是运行时事实
- Token 来源文件：现 `frontend/src/style.css` 的 `:root` CSS 变量（迁移映射见下表）；接入后来源变为 `style.css` 中的 `@theme` 块
- 提取限制：`@theme` 迁移表为**已决策的规划值**，颜色值与现状逐一对应、不新造色

## 颜色（Tailwind @theme 映射）

| Tailwind token（@theme 变量） | 值 | 来源 | utility 示例 |
| --- | --- | --- | --- |
| `--color-canvas` | `#f6f7f9` | 原 `--bg` 迁移 | `bg-canvas` |
| `--color-ink` | `#1f2329` | 原 `--text` 迁移 | `text-ink` |
| `--color-ink-secondary` | `#646a73` | 原 `--text-secondary` 迁移 | `text-ink-secondary` |
| `--color-line` | `#e5e6eb` | 原 `--border` 迁移 | `border-line` |
| `--color-primary` | `#3370ff` | 原 `--primary` 迁移 | `bg-primary` / `text-primary` |
| `--color-primary-hover` | `#2b5fd9` | 原 `--primary-hover` 迁移 | `hover:bg-primary-hover` |
| `--color-success` | `#34c724` | 原 `--success` 迁移 | `text-success` |
| `--color-danger` | `#f54a45` | 原 `--danger` 迁移 | `text-danger` |
| `--color-sidebar` | `#304156` | RuoYi 参照（管理端布局引入） | `bg-sidebar` |
| `--color-sidebar-hover` | `#263445` | RuoYi 参照 | `hover:bg-sidebar-hover` |
| `--color-sidebar-active` | `#1f2d3d` | RuoYi 参照（选中菜单底色） | `bg-sidebar-active` |
| `--color-sidebar-text` | `#bfcbd9` | RuoYi 参照（侧栏文字） | `text-sidebar-text` |

卡片/头部背景 `#ffffff` 直接用默认 `white`，不自定义。Tailwind v4 默认色板保留可用，业务语义色一律走上表自定义 token。

## 字体排印

| Token | 值 / 来源 | 说明 |
| --- | --- | --- |
| `--font-sans` | `'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif` | 原根字体栈迁移，`body` 默认应用 |
| 字号策略 | 采用默认刻度：`text-sm`(14px) / `text-base`(16px) / `text-lg`(18px) | 既有 13px 辅助文本就近统一到 `text-sm`；任意值语法（如 `text-[13px]`）仅作兜底不滥用 |

## 间距与尺寸

| 类别 | 约定 | 说明 |
| --- | --- | --- |
| 间距 | Tailwind 默认 4px 基准刻度 | 既有字面值就近映射：12px→`gap-3`、16px→`p-4`、20px→`p-5`、24px→`p-6`、32px→`p-8` |
| 内容宽度 | `max-w-[960px]` | 原 `.main` 的 960px 上限 |

## 圆角、阴影、层级与动效

| Token / 约定 | 值 / 来源 | 说明 |
| --- | --- | --- |
| `--radius-card` | `10px` | 原卡片圆角迁移 → `rounded-card`；按钮/小元素用默认 `rounded-md`(6px) |
| 阴影 | 默认刻度（`shadow-sm` 等） | 原项目未用阴影，不强造 token |
| z-index 层级 | 默认刻度 | 未识别（项目未定义层级体系） |
| 动效 | `--animate-shake`（错误抖动 0.4s）、`--animate-fade-up`（页面/卡片入场 0.35s，视图切换即新页入场动画）、`.pop-*`（弹层缩放淡入 0.15s，配 Vue Transition）、全局按钮 `active:scale-[0.97]` | 已有事实（场景 01 交互优化引入；`prefers-reduced-motion` 下全局降级。注：不使用组件级路由 Transition——out-in 与懒加载视图组合在该环境会偶发卡空，改为视图根元素入场动画） |

## 未识别与建议

- 表单校验态（错误边框/提示文案）落地时用 `border-danger` + `text-danger` 组合，不新增全局类；
- 本表在 Tailwind 接入任务完成后由"迁移映射"转为"已有事实"并同步更新此说明。

## 同步规则

修改 `@theme` 块或 Tailwind 相关配置后，同步更新本表。除非需求明确要求，不为生成本文档重构既有样式或引入其他 CSS 框架。
