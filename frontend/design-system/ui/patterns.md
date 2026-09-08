# UI · 样式配方

> 这些是当前页面已有的 utilities 组合，不是已封装的组件 API。片段只说明外观，实际状态与事件由业务页提供。变量、请求和校验不能靠复制样式自动获得。

## 1. 操作层级

来源：[LoginView.vue](../../src/views/LoginView.vue)、[AuthView.vue](../../src/views/scenario/AuthView.vue)、[FlashSaleView.vue](../../src/views/scenario/FlashSaleView.vue)。

| 层级 | 配方 | 使用场景 |
| --- | --- | --- |
| 主操作 | `bg-primary text-white hover:bg-primary-hover` | 登录、发送验证、开始实验 |
| 次操作 | `border border-line hover:bg-code-bg` | 刷新、重置入口、取消 |
| 轻操作 | `bg-primary-soft text-primary` | 本地计数、筛选选中 |
| 文本入口 | `text-primary hover:underline` | 原理链接、快捷填充 |
| 危险确认 | `bg-danger text-white` | 明确确认清订单后的执行按钮 |

```html
<button type="button" class="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-xs font-medium text-white hover:bg-primary-hover disabled:opacity-60">
  发送身份验证请求
</button>
```

示例不带事件，因此不是可直接投产的交互。真实按钮使用 `:disabled` 与 loading 文案；在 form 中明确 type，导航则使用 RouterLink。

默认按钮有 150ms 过渡、按下 scale(.98)。秒杀保留部分 rounded-md 控件，不假装全站都已经替换成 rounded-lg。

## 2. 图标配文字

来源：[AppIcon.vue](../../src/components/AppIcon.vue)、[AdminLayout.vue](../../src/layouts/AdminLayout.vue)。

- 默认 16px，常用 gap-2；小辅助图标 12/14px，大卡片图标 20px。
- 装饰图标 aria-hidden；文字仍承担用途说明。
- 当前选中颜色由外层 text-primary 传递。
- 只有图标的按钮需要 aria-label，不能靠 title 或图形形状代替。

```vue
<button type="button" aria-label="打开场景导航" class="rounded-lg p-2 text-ink-secondary">
  <AppIcon name="menu" class="h-5 w-5" />
</button>
```

该片段假定调用方已从 `@/components/AppIcon.vue` 导入组件；开关逻辑在 AdminLayout 中，并不包含在图标里。

## 3. 表单字段

来源：[LoginView.vue](../../src/views/LoginView.vue)、[RegisterView.vue](../../src/views/RegisterView.vue)。

```html
<label class="block">
  <span class="mb-2 block text-xs font-medium">用户名</span>
  <input type="text" required autocomplete="username"
    class="w-full rounded-lg border border-line bg-canvas/60 px-3 py-3 text-sm focus:border-primary focus:outline-none"
    placeholder="请输入用户名" />
</label>
```

- label 在上、输入在下；字号 12/14px，间距 8px。
- 登录字段 py-3，注册字段 py-2.5；密码右侧按钮对应 pr-11，避免覆盖内容。
- 错误面板使用 `role=alert`、`bg-danger/5 text-danger`，padding 12×8px，圆角 8px。
- 登录错误目前同时关联两个字段；注册目前没有字段级 aria 关联。未来扩展可改善，但不能在资料中写成已完成。
- 原生 required/pattern/minlength/maxlength 仍参与提交约束。
- 显示密码按钮独立 `type=button`，不触发表单提交。

## 4. 搜索与筛选

来源：[HomeView.vue](../../src/views/HomeView.vue)。

- 搜索容器：`flex items-center gap-2 rounded-lg border border-line bg-paper px-3 py-2`，focus-within 主色边框。
- 输入：`type=search`，`min-w-0 flex-1 bg-transparent text-xs outline-none`。
- 筛选按钮：`rounded-md px-3 py-2 text-xs`，当前 `bg-primary-soft font-medium text-primary`。
- 筛选用 aria-pressed；旁边数量字体等宽、10px。
- 搜索无网络 loading，输入变化立即过滤已有元信息。

## 5. 场景卡片

来源：[HomeView.vue](../../src/views/HomeView.vue)。

```text
左上图标容器                         右上收录状态
编号 / 分类
标题
一至两段简短说明
技术标签
───────────────────────────────────────────
阅读文档       交互演示                     箭头
```

外框：`flex flex-col rounded-xl border border-line p-5 sm:p-6`。启用卡片白底及主色 hover 边框，规划卡片 canvas 底。标签行 mt-auto，动作行 border-t，使同排卡片操作位置稳定。

- 不是整卡 clickable；标题与两个动作链接各自可访问。
- 状态徽标包含文字和圆点，不能只显示绿点。
- tags 是元信息标签，不是可以筛选的按钮。
- 规划项替换底部操作为静态未开放说明，不呈现假链接。

## 6. 通用实验面板

来源：[AuthView.vue](../../src/views/scenario/AuthView.vue)、[FlashSaleView.vue](../../src/views/scenario/FlashSaleView.vue)。

```html
<section class="rounded-xl border border-line bg-paper p-5 sm:p-6">
  <p class="mb-3 font-mono text-[10px] tracking-widest text-primary">01 / IDENTITY</p>
  <h2 class="text-base font-semibold">验证当前身份</h2>
  <p class="mt-2 text-xs leading-6 text-ink-secondary">说明操作目的、接口边界与结果含义。</p>
</section>
```

真实面板在标题后安排输入/动作和结果。不要为每一行数据再嵌套一层大卡片；轻信息优先 code-bg 或边框小区域。

## 7. 状态与结果

| 状态 | 已有视觉 | 语义来源 |
| --- | --- | --- |
| 初始等待 | 虚线边框、居中辅助文字 | 身份响应与订单空态 |
| 加载 | 按钮禁用、文案变化、spin；列表用 pulse 骨架 | 各页面局部状态 |
| 成功 | success/5 背景、success/20 边框、文字/勾图标 | 身份结果 |
| 错误 | danger/5 背景、danger 文字、重试动作 | 表单及订单 |
| 无搜索结果 | 虚线框、说明、清除筛选按钮 | HomeView |
| 危险确认 | danger/20 边框、danger/5 背景、确认/取消 | 秒杀重置 |

提示框是当前页面的内容，不是自动全局消息系统。status/alert 应放在实际状态区域；不要给持续刷新的整个页面设置 live region。

## 8. 标签与元信息

- 轻标签：`rounded border border-line px-2 py-0.5 text-[10px] text-ink-secondary`。
- 状态胶囊：`rounded-full bg-primary-soft px-2 py-1 text-[10px] text-primary`。
- 英文 eyebrow：等宽/10px/字距 0.15–0.2em；中文正文不使用宽字距。
- 用户头像：圆形 32px、primary-soft 底、primary 字色；首字符不是用户上传头像。
- API 片段：`bg-code-bg` 小面板 + 等宽路径，GET 主色强调；不是可编辑终端。

## 9. 文档正文与代码

来源：[DocView.vue](../../src/views/scenario/DocView.vue)、[renderDoc.js](../../src/utils/renderDoc.js)。

- 正文使用 Typography modifiers，不新增 `.doc-prose` 全局样式。
- 引用：浅绿底 + 左侧主色线，非斜体；图片轻边框与白底。
- h2 使用上边框和较大间距，第一直接子 h2 有例外处理。
- 行内代码为 code-bg、主色、轻 padding，不带 Typography 默认反引号装饰。

代码块结构由 renderDoc 生成：

```text
┌──────── 语言标签 ───────────────── 复制代码 ───────┐
│ 深色 code-panel                                  │
│ 原文等宽、12px、24px 行高、内部横向滚动             │
└──────────────────────────────────────────────────┘
```

代码语言标签只用于显示；没有高亮、行号或折叠。复制使用独立保存的 codes 原文，不从被转义 HTML 拼回。

表格外框 rounded-xl，最小 480px，表头 code-bg；cell px-4 py-3，所有列左对齐。横向滚动只发生在表格外壳。

## 10. 弹层与局部反馈

- 账户弹层：absolute、右对齐、宽 192px，border-line、paper、rounded-xl、p-2、shadow-lg，使用 pop Transition。
- 复制提示：fixed bottom-6，居中，max-w 90vw，bg-ink/text-white、rounded-xl、shadow-lg，3 秒后消失。
- 移动抽屉：真实页面导航状态，不是通用 Modal 组件。
- 重置确认：内联面板，无 overlay，不应从其配方推导出焦点约束。

新增这些模式时必须同时核对交互和层级，不只复制颜色。详见 [interactions.md](./interactions.md)。
