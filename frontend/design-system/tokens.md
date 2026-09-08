# Token 清单 · 当前样式事实

> 运行时来源：[src/style.css](../src/style.css) 的 Tailwind v4 `@theme`。本表是人类可读词汇表，不是另一个配置文件。

表内区分 **项目 Token**、**Tailwind 默认尺度**、**页面局部值**。后两者不能被误读成项目已经定义了同名 CSS 变量。rem 换算按浏览器根字号 16px 说明，用户调整根字号后应以 rem 为准。

## 1. 项目颜色 Token

以下 14 项均为已定义事实，CSS 变量使用 `--color-` 前缀。

| CSS 变量 | 值 | utility 示例 | 语义 / 当前状态 |
| --- | --- | --- | --- |
| `--color-canvas` | `#f7f8f5` | `bg-canvas` | 纸感页面底色 |
| `--color-paper` | `#ffffff` | `bg-paper` | 卡片、表单、菜单 |
| `--color-ink` | `#202923` | `text-ink` | 标题、重点正文 |
| `--color-ink-secondary` | `#67736b` | `text-ink-secondary` | 说明、元信息 |
| `--color-line` | `#e2e7df` | `border-line` | 细边框、分隔线 |
| `--color-primary` | `#28634b` | `bg-primary` / `text-primary` | 松绿主操作、当前导航 |
| `--color-primary-hover` | `#1d4e39` | `hover:bg-primary-hover` | 主操作悬停 |
| `--color-primary-soft` | `#eaf1e9` | `bg-primary-soft` | 轻操作、导航选中、提示 |
| `--color-success` | `#287a55` | `text-success` | 成功反馈 |
| `--color-danger` | `#bc4545` | `text-danger` / `bg-danger` | 错误、危险确认；秒杀价格沿用此色 |
| `--color-code-bg` | `#f0f3ed` | `bg-code-bg` | 行内代码、代码栏、轻底色 |
| `--color-code-panel` | `#1e2d25` | `bg-code-panel` | 代码块深色底 |
| `--color-code-text` | `#dfebe2` | `text-code-text` | 代码块正文 |
| `--color-amber` | `#976723` | `text-amber` | 已定义，当前未见业务使用 |

### 配对方式（已有用法）

- 主按钮：`bg-primary text-white hover:bg-primary-hover`。
- 轻操作：`bg-primary-soft text-primary`。
- 错误：`bg-danger/5 text-danger`，危险边框 `border-danger/20`。
- 成功结果：`bg-success/5 text-success border-success/20`。
- 次要面板：`bg-canvas/60`；骨架及轨道：`bg-code-bg` 或 `bg-line`。
- 遮罩：`bg-ink/30 backdrop-blur-sm`。
- 品牌块：`bg-primary text-white`，辅助文字使用白色透明度。

透明度组合并非独立 Token。颜色必须结合文字/状态标签表达含义；本表不代表所有组合均通过 WCAG 对比度测量。

## 2. 字体 Token

| CSS 变量 / utility | 当前值 | 类型 | 使用 |
| --- | --- | --- | --- |
| `--font-sans` | `'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif` | 项目 Token | body 默认，阅读及控件 |
| `--font-display` | `'Georgia', 'Songti SC', 'SimSun', serif` | 项目 Token | 首页、认证品牌文案 |
| `font-mono` | Tailwind 默认等宽栈，未覆盖 | 默认尺度 | 编号、代码、英文元信息 |

未发现 `@font-face` 或字体文件加载。声明 Inter 不保证每台设备都实际使用 Inter；不得把字体回退说成远程字体已经安装。

### 字号与字距

| 用法 | 尺寸 | 类型 |
| --- | --- | --- |
| `text-[10px]` / `text-[11px]` / `text-[13px]` / `text-[15px]` | 对应像素值 | 页面已有局部值，主要用于元信息及紧凑演示 |
| `text-xs` | 0.75rem / 12px | 默认尺度 |
| `text-sm` | 0.875rem / 14px | 默认尺度 |
| `text-base` | 1rem / 16px | 默认尺度 |
| `text-lg` / `text-xl` | 18px / 20px | 默认尺度 |
| `text-2xl` / `text-3xl` | 24px / 30px | 默认尺度 |
| `text-4xl` / `text-5xl` | 36px / 48px | 默认尺度 |
| `tracking-tight` | -0.025em | 默认尺度，标题 |
| `tracking-widest` | 0.1em | 默认尺度，编号 |
| `tracking-[0.15em]`、`[0.18em]`、`[0.2em]` | 对应 em 值 | 页面已有局部值，英文 eyebrow |

正文常用 `leading-6`＝24px、`leading-7`＝28px；文档正文使用后者。首页主标题为 `text-3xl sm:text-5xl leading-[1.4]`；场景标题为 `text-3xl sm:text-4xl`。不要把所有小字号元信息提升为统一正文标准。

## 3. 间距与响应式

### 默认 spacing

Tailwind 默认单位 0.25rem＝4px；当前没有额外 spacing 变量。

| 类示例 | 尺寸 | 已有用途 |
| --- | --- | --- |
| `gap-1` / `gap-1.5` / `gap-2` | 4 / 6 / 8px | 标签和图标间距 |
| `gap-3` / `gap-4` | 12 / 16px | 控件组、卡片间距 |
| `p-5` / `p-6` | 20 / 24px | 常见卡片 |
| `p-7` / `p-8` / `p-10` / `p-12` | 28 / 32 / 40 / 48px | 页面与表单区 |
| `gap-9` | 36px | 文档正文与目录间距 |
| `px-14` | 56px | xl 主内容水平留白 |

### 默认断点

项目没有覆盖 Tailwind 断点。

| 断点 | 默认值 | 常用转换 |
| --- | --- | --- |
| sm | 40rem | 640px，场景卡片双列 |
| md | 48rem | 768px，认证双列、商品双列 |
| lg | 64rem | 1024px，显示桌面侧栏 |
| xl | 80rem | 1280px，桌面文档目录与首页方法栏 |
| 2xl | 96rem | 1536px，默认存在，当前核心布局未使用 |

### 页面尺寸（不是自定义 Token）

| 元素 | 当前值 | 来源 |
| --- | --- | --- |
| 应用框架 | `max-w-[1600px]` | AdminLayout |
| 内容上限 | `max-w-[1120px]` | AdminLayout |
| 顶栏 | `h-18`＝72px | AdminLayout |
| 桌面侧栏 | `w-64`＝256px | AdminLayout |
| 手机抽屉 | `w-68`＝272px | AdminLayout |
| 认证外框 | `max-w-5xl`＝64rem | AuthShell |
| 首页方法栏 | 240px | HomeView |
| 桌面文档目录 | 176px | DocView |
| 文档表格 | `min-w-[480px]` | renderDoc |
| 默认图标 | `h-4 w-4`＝16px | AppIcon |

## 4. 圆角与边框

| Token / utility | 值 | 当前状态 |
| --- | --- | --- |
| `--radius-card` | 14px | 项目 Token 已定义；当前模板未使用 `rounded-card` |
| `rounded` | 4px | Tailwind 默认，小按钮或行内元素 |
| `rounded-md` | 6px | 默认，秒杀页部分控件 |
| `rounded-lg` | 8px | 默认，多数按钮/输入/轻面板 |
| `rounded-xl` | 12px | 默认，卡片、菜单和代码容器 |
| `rounded-2xl` | 16px | 默认，认证外框 |
| `rounded-full` | 胶囊/圆形 | 默认，头像、状态徽标 |

**当前常见卡片 12px，并非 radius-card 的 14px。** 不为了统一文档而擅自修改现有类。边框通常 1px；选中页内导航下边框与目录左边框为 2px。

## 5. 阴影与层级

没有项目级阴影 Token。`shadow-lg` 来自 Tailwind 默认，当前用于账户弹层与复制提示；常规卡片不靠大面积阴影分层。

| 局部层级 | 数值 / 类 | 说明 |
| --- | --- | --- |
| 桌面侧栏 | z-10 | 主体框架内 |
| sticky 顶栏 | z-30 | 形成自身 stacking context |
| 账户遮挡层 | z-30 | 顶栏内部 |
| 账户触发按钮及弹层 | z-40 | 顶栏内部，不能直接与全局 z-40 比大小 |
| 移动抽屉遮罩 | z-40 | 页面根层 |
| 移动抽屉、跳至内容链接 | z-50 | 页面根层 |
| 文档复制反馈 | z-40 | 页面内固定提示 |

没有统一 overlay 管理器。新增弹层应检查其祖先 stacking context，而不是只增加 z-index 数字。

## 6. 动效与焦点

| 项目 | 当前实现 | 类型 |
| --- | --- | --- |
| `--animate-fade-up` | `fade-up 0.35s ease-out both`；透明度与 translateY(8px) 入场 | 项目 Token，页面使用 |
| `--animate-shake` | `shake 0.4s ease-in-out` | 项目 Token，已定义但当前未使用 |
| `pop` | 0.15s opacity/transform；起止态 translateY(-4px)、scale(.98) | 现有手写 Vue 过渡类，账户菜单使用 |
| button | transition-all 150ms，active scale(.98) | base 层 |
| focus-visible | 主色 2px outline、offset 4px | base 层；输入与 main 有局部覆盖 |
| reduced-motion | animation/transition duration .01ms，scroll-behavior auto | base 层降级 |

`animate-spin`、`animate-pulse` 使用 Tailwind 默认，用于局部 loading/骨架。移动抽屉目前条件显示/隐藏，没有滑入动画；正文 hash 定位也未统一启用 smooth scroll。

## 7. 未实现与维护限制

- 无暗色主题、动态换肤、主题持久化。
- 无独立 elevation、breakpoint、spacing 自定义 Token。
- 无自动对比度检测、字体加载保证或全站无障碍认证。
- Token 定义存在不等于已在页面使用，新增文档需保留“预留/未使用”的区分。
- `sources.json` 记录源文件校验值；主题或页面局部值变化后同步本文，不把本文作为运行时代码导入。
