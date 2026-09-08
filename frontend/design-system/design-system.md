# 设计系统 · 视觉方向与组件清单

> 当前实现的参考快照。颜色细节见 [tokens.md](./tokens.md)，开发约束见 [rules.md](./rules.md)。本目录不参与运行时构建。

## 1. 产品与视觉定位

reprise 是可运行的工程场景记录，不是数据管理后台。用户有三条主要路径：

1. 在场景集合发现问题与技术主题。
2. 阅读技术文档，理解架构、契约、取舍与边界。
3. 进入交互演示，通过真实操作验证行为。

视觉重心是内容而不是装饰：纸感浅底、松绿主操作、石墨标题、低对比细分隔线，留白区分信息层级。衬线字体用于品牌主视觉；无衬线用于阅读与控件；等宽字用于编号、技术元信息和代码。

### 已有外观语言

- 页面背景 `canvas`，信息容器 `paper`，默认 `line` 边框。
- 导航选中、轻提示与技术标签强调使用 `primary-soft` 和 `primary`。
- 正文说明使用 `ink-secondary`，标题/重点使用 `ink`。
- 卡片优先用边框和底色分层；阴影集中于账户弹层与复制反馈。
- 场景页标题、描述、面包屑、文档/演示切换由布局统一提供。
- 没有暗色主题，也没有集中式组件 variant 系统。

## 2. 技术与组件总表

技术栈：Vue 3 + `<script setup>` + JavaScript ESM；Vite 8；Tailwind v4 + Typography；Vue Router / Pinia / Axios / Marked。无外部 UI 组件库。

| 组件 | 用途 | 源码 | 来源 |
| --- | --- | --- | --- |
| AppIcon | 线性图标 | [AppIcon.vue](../src/components/AppIcon.vue) | 自有 |
| AuthShell | 登录、注册共用品牌外框 | [AuthShell.vue](../src/components/AuthShell.vue) | 自有 |
| AdminLayout | 已登录场景知识库骨架 | [AdminLayout.vue](../src/layouts/AdminLayout.vue) | 自有 |

**公共组件共 3 个**。路由页面、Markdown 转换函数不计为公共视觉组件。当前没有 Button、Input、Card、Badge、Tabs、Toast、Modal、EmptyState、Skeleton、Progress 或 FormField 组件；对应外观是页面内 utilities 组合，参见 [UI 配方](./ui/patterns.md)。

## 3. AppIcon

- **源码**：[src/components/AppIcon.vue](../src/components/AppIcon.vue)。
- **用途**：统一线性 SVG，支持导航、状态与动作图标。
- **Props**：`name: String`，默认 `grid`；未知名称回退 `grid`。
- **Emits**：无显式声明。
- **Slots**：无。
- **其他接口**：未定义 v-model、expose、size、color 或 variant prop；尺寸和颜色通过根节点普通 class 合并。
- **何时用**：图标配文字的导航/按钮、辅助装饰图标。
- **何时不用**：复杂业务示意图、带文本的架构图；这类资源仍使用场景 SVG 文件。
- **相似组件辨析**：它是内联 path SVG，不是公共目录 `icons.svg` 的 sprite 引用器。

实现参数：24×24 viewBox、`fill=none`、`stroke=currentColor`、描边 1.6、圆端点/连接，默认 `h-4 w-4 shrink-0`。颜色来自 currentColor，尺寸默认约 16px，不自动随父级字号变化。

图标名称：`grid`、`search`、`arrow`、`book`、`play`、`code`、`chevron`、`menu`、`close`、`check`、`copy`、`lock`、`bolt`、`logout`、`eye`、`refresh`。

```vue
<script setup>
import AppIcon from '@/components/AppIcon.vue'
</script>

<template>
  <RouterLink to="/" class="inline-flex items-center gap-2 text-xs text-primary">
    <AppIcon name="grid" class="h-4 w-4" />
    场景集合
  </RouterLink>
</template>
```

默认 `aria-hidden=true`，没有内部 title 或名称 prop。只有图标的按钮必须由按钮提供可访问名称；不要依赖装饰图标给读屏器解释用途。

## 4. AuthShell

- **源码**：[src/components/AuthShell.vue](../src/components/AuthShell.vue)。
- **用途**：认证页面共享的左右品牌布局，小屏变成单列。
- **Props**：`title`、`subtitle`、`eyebrow` 均为 String，非 required，无显式默认值，未传入时是 `undefined`。
- **Emits**：无显式声明。
- **Slots**：仅默认 slot，无 slot props；用于放入表单与底部入口。
- **其他接口**：没有命名 slot、v-model 或 expose。
- **何时用**：LoginView、RegisterView 等独立认证页面。
- **何时不用**：已登录场景页、包含侧栏的文档页、通用弹窗。
- **相似组件辨析**：不是表单组件，不负责验证、提交、错误状态或 loading。

最大宽度 64rem，外框 rounded-2xl；md 起两等分列，左侧松绿品牌区；右侧表单 padding 随断点变化，详见 [布局规范](./ui/layouts.md)。左侧品牌 slogan 和标签为固定内容，没有配置入口。

```vue
<script setup>
import AuthShell from '@/components/AuthShell.vue'
</script>

<template>
  <AuthShell title="回到场景库" subtitle="登录后继续探索。" eyebrow="WELCOME BACK">
    <p class="text-sm leading-6 text-ink-secondary">此处由页面放置自己的表单与提交反馈。</p>
  </AuthShell>
</template>
```

三个文本元素没有条件隐藏；不能认为缺省 prop 会自动收起布局。

## 5. AdminLayout

- **源码**：[src/layouts/AdminLayout.vue](../src/layouts/AdminLayout.vue)。
- **用途**：顶栏、账户菜单、场景目录、移动抽屉、标题与文档/演示导航、页脚。
- **Props**：无。
- **Emits**：无显式声明。
- **Slots**：无；内部 `<RouterView />` 是路由内容出口，**不是默认 slot**。
- **状态来源**：Vue Router 当前路径、`useAuthStore()`、静态 scenarios 注册表。
- **何时用**：`/` 布局路由下的首页与场景页面。
- **何时不用**：登录/注册独立页、通用对话框。
- **相似组件辨析**：它是有路由和账号依赖的应用框架，不是可随意包裹内容的 Card/Shell。

桌面侧栏宽 256px，顶栏高 72px，外框最大 1600px、内容最大 1120px。移动端抽屉宽 272px，支持开关、遮罩、Escape、首尾 Tab 循环。完整尺寸与状态边界分别见 [布局](./ui/layouts.md) 和 [交互](./ui/interactions.md)。

后续场景子页面避免重复 h1；文档页 Markdown h1 由渲染器省略。文档/演示入口是 RouterLink，不是已经实现方向键交互的 ARIA Tabs。

## 6. 页面与渲染能力

| 对象 | 已实现职责 | 源码 |
| --- | --- | --- |
| HomeView | 搜索、状态筛选、双入口卡片、空态 | [HomeView.vue](../src/views/HomeView.vue) |
| LoginView | 真实登录、演示账号填充、密码显隐 | [LoginView.vue](../src/views/LoginView.vue) |
| RegisterView | 注册即登录、用户名/密码校验 | [RegisterView.vue](../src/views/RegisterView.vue) |
| AuthView | 身份/健康请求、本地 Pinia 计数 | [AuthView.vue](../src/views/scenario/AuthView.vue) |
| FlashSaleView | 商品、订单、并发实验、危险重置确认 | [FlashSaleView.vue](../src/views/scenario/FlashSaleView.vue) |
| DocView | `scenarioId: String` 必填；文档与响应式目录、阅读/复制反馈 | [DocView.vue](../src/views/scenario/DocView.vue) |
| renderDoc | 受信 Markdown → `{ html, headings, codes }` | [renderDoc.js](../src/utils/renderDoc.js) |

除 DocView 的必填 scenarioId 外，这些页面无对外 props/slots 契约。renderDoc 是纯转换工具，不是 Vue 组件；代码转义不等于全文 HTML 消毒。

## 7. 场景接入契约

同时维护以下三个位置：

1. [scenarios.js](../src/config/scenarios.js)：`id/no/name/desc/tags/category/icon/path/enabled`。
2. [router/index.js](../src/router/index.js)：演示与 `/doc` 路由成对登记，文档路由传 scenarioId。
3. [scenarioDocs.js](../src/config/scenarioDocs.js)：场景 `design.md?raw` 与 SVG `?url` 映射。

只有文档和演示均可用时开启 enabled。该字段不会自动生成路由，也没有插件自动发现机制。当前首页、文档和演示均要求登录，不应把新样式称为匿名可访问站点。

新组件或元信息变更需同步本清单，不能靠更新参考文档假定业务已经完成。
