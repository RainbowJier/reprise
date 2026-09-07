---
id: auth-jwt_12_ui_scenario_doc_menu
name: "场景菜单二级化（详情页 + 技术说明文档页）"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_11_ui_admin_layout]
profiles: [frontend]
files:
  - frontend/package.json
  - frontend/vite.config.js
  - frontend/src/style.css
  - frontend/src/config/scenarioDocs.js
  - frontend/src/config/scenarios.js
  - frontend/src/layouts/AdminLayout.vue
  - frontend/src/views/scenario/DocView.vue
  - frontend/src/router/index.js
  - frontend/design-system/design-system.md
  - AGENTS.md
---

# 场景菜单二级化（详情页 + 技术说明文档页）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui`（增量：管理端布局二级菜单与文档渲染） |
| 已启用 profile | frontend |
| 架构边界 | 前端菜单/路由/文档渲染；md 与 SVG 数据源为仓库根 `scenarios/01-auth/`（Vite `?raw`/`?url` 导入） |
| 结构分析 | 文档内容为本仓库自有文件（可信源，v-html 渲染不引入外站内容）；md 内图片引用为相对路径 `auth-jwt/*.svg`，渲染前按 assets 映射重写 |
| 完成条件 | `npm run build` 绿；浏览器验证二级菜单展开/跳转、文档页 markdown 渲染与三张 SVG 展示 |

## 需求与验收

- 用户目标：每个场景一个父菜单，下挂「详情页效果展示」「技术说明文档」两个子菜单；文档页解析场景 md 并展示 SVG 配图。
- 包含：手风琴父菜单、通用 DocView（marked + prose）、scenarioDocs 注册模块、路由子项、fs.allow、文档约定写入 AGENTS。
- 不包含：NOTES.md 展示（后续可加）、md 内 mermaid 块的图渲染（保留为代码块，视觉配图以 SVG 为准）。
- 验收：菜单展开收起正常；两个子菜单高亮与路由联动；文档页标题/表格/代码块/SVG 渲染正确；未登录访问文档页被拦。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 数据源 = `scenarios/01-auth/auth-jwt.md` + `auth-jwt/*.svg`（research-svg 产物） |
| 数据/Schema | 不适用 |
| 集成 | 新依赖 `marked`（md→html）与 `@tailwindcss/typography`（prose 排课）；dev 需 `server.fs.allow` 放开仓库根 |
| 安全与质量 | v-html 内容全部来自仓库自有 md（可信），渲染前仅重写图片 URL；文档页在 requiresAuth 布局内 |
| 依赖 | 11（管理端布局，已完成） |

## 实现步骤

1. `npm install marked @tailwindcss/typography`；
2. `vite.config.js` 增 `server.fs.allow`（仓库根）；
3. `style.css` 挂 typography 插件；
4. 新建 `config/scenarioDocs.js`（md `?raw` + svg `?url` 注册）；
5. AdminLayout 菜单二级化（手风琴 + 子菜单）；
6. 新建 DocView 通用文档页；router 增文档子路由；
7. AGENTS/design-system 同步；build + 浏览器验证。

## 涉及文件

见 front matter `files`。

## 完整代码（供手动敲写）

### frontend/vite.config.js（修改）

```javascript
import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 允许读取仓库根 scenarios/ 下的 md/svg（技术文档页数据源：?raw / ?url 导入）
    fs: {
      allow: [fileURLToPath(new URL('..', import.meta.url))],
    },
    proxy: {
      // 后端 context-path 为 /api，无需 rewrite
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### frontend/src/style.css（修改，@import 之后追加插件挂载）

```css
@import 'tailwindcss';
@plugin '@tailwindcss/typography';
```

### frontend/src/config/scenarioDocs.js（新增）

```javascript
// 场景技术文档注册表：md 经 ?raw 内联、SVG 经 ?url 打包为资源
// 新场景在此登记：markdown 原文 + md 内相对图片路径 → 导入 URL 的映射
import authJwtMd from '../../../scenarios/01-auth/auth-jwt.md?raw'
import archSvg from '../../../scenarios/01-auth/auth-jwt/auth-architecture.svg?url'
import flowSvg from '../../../scenarios/01-auth/auth-jwt/dual-token-flow.svg?url'
import jwtSvg from '../../../scenarios/01-auth/auth-jwt/jwt-structure.svg?url'

export const scenarioDocs = {
  '01-auth': {
    markdown: authJwtMd,
    assets: {
      'auth-jwt/auth-architecture.svg': archSvg,
      'auth-jwt/dual-token-flow.svg': flowSvg,
      'auth-jwt/jwt-structure.svg': jwtSvg,
    },
  },
}
```

### frontend/src/views/scenario/DocView.vue（新增）

```vue
<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

import { scenarioDocs } from '@/config/scenarioDocs'

const props = defineProps({
  scenarioId: { type: String, required: true },
})

const doc = computed(() => scenarioDocs[props.scenarioId])

const html = computed(() => {
  if (!doc.value) {
    return '<p>该场景暂无技术文档。</p>'
  }
  let md = doc.value.markdown
  // md 内相对图片路径重写为 Vite 资源 URL（内容为仓库自有文件，可信源）
  for (const [rel, url] of Object.entries(doc.value.assets)) {
    md = md.replaceAll(`](${rel})`, `](${url})`)
  }
  return marked.parse(md)
})
</script>

<template>
  <section class="rounded-card border border-line bg-white px-8 py-6">
    <article class="prose prose-sm max-w-none prose-img:mx-auto prose-img:rounded" v-html="html" />
  </section>
</template>
```

### frontend/src/router/index.js（修改，布局 children 追加文档子路由）

```javascript
      children: [
        {
          path: 'scenario/01-auth',
          name: 'scenario-01-auth',
          component: () => import('@/views/scenario/AuthView.vue'),
          meta: { title: '用户登录与认证' },
        },
        {
          path: 'scenario/01-auth/doc',
          name: 'scenario-01-auth-doc',
          component: () => import('@/views/scenario/DocView.vue'),
          props: { scenarioId: '01-auth' },
          meta: { title: '用户登录与认证 · 技术文档' },
        },
        // 后续场景：详情页 + /doc 文档页成对追加，并在 config/scenarios.js、config/scenarioDocs.js 登记
      ],
```

### frontend/src/layouts/AdminLayout.vue（修改，菜单二级化）

菜单区替换为手风琴结构：父项（点击展开/收起，规划中场景置灰不可展开），子项两个 RouterLink（详情页效果展示 `s.path`、技术说明文档 `s.path + '/doc'`），活动子项高亮、其父项同步高亮；面包屑追加子菜单名。完整代码见仓库实现（本任务代码基准）。

### AGENTS.md（修改）

管理端布局条目追加：每个场景父菜单下两个子菜单——「详情页效果展示」（`/scenario/NN-xxx`）与「技术说明文档」（`/scenario/NN-xxx/doc`，通用 DocView 解析 md）；新场景需在 `src/config/scenarioDocs.js` 登记文档与 SVG 资源映射。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 构建成功（md 内联、svg 打包） |
| manual | 登录后点击父菜单展开/收起；两个子菜单跳转与高亮 | 正常联动 |
| manual | 文档页 | 标题/表格/代码块/三张 SVG 渲染正确 |
| manual | 未登录访问 `/scenario/01-auth/doc` | 跳登录页带 redirect |

## 风险与阻塞

- 风险：`v-html` 渲染仓库自有 md（可信源，无外站内容）；mermaid 代码块按代码呈现（视觉图以 SVG 为准）。
- 阻塞：无。
- 执行记录：2026-09-05 落盘全部文件（marked + typography 依赖、fs.allow、scenarioDocs、二级菜单 AdminLayout、DocView、doc 路由）；`npm run build` 成功（3 个 SVG 打包为 hash 资产、md 内联 DocView chunk）；浏览器验证通过——父菜单手风琴展开、两子菜单高亮联动、面包屑三级、文档页 h1/6 表格/1 代码块/3 张 SVG 全部渲染；视觉截图因浏览器截图接口 3s 上限超时跳过（DOM 断言已确认）。

修订：1 - 用户反馈「左侧菜单高度固定」：AdminLayout 外层由 `min-h-screen` 改 `h-screen overflow-hidden`——侧栏固定为视口高（菜单区内部滚动），右侧内容区独立滚动，长文档页不再撑长侧栏。实测 aside 高度=视口高、main 可滚、windowScrollY=0。

修订：2 - 用户反馈文档页不应展示过程性需求文档（auth-jwt.md），重新撰写独立的**场景设计文档** `scenarios/01-auth/design.md`（背景/总体设计/认证流程/数据模型/接口契约/关键取舍/安全边界/验证结果/延伸方向 九章，配同三张 SVG），scenarioDocs.js 数据源切换为 design.md；AGENTS 约定同步为"文档页展示 design.md"。实测渲染 9 章节 + 6 表格 + 3 SVG。auth-jwt.md 保留为工作流过程产物不展示。
