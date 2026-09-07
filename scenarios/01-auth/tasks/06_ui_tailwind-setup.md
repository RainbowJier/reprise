---
id: auth-jwt_06_ui_tailwind_setup
name: "前端 Tailwind CSS v4 接入与存量样式改写"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: []
profiles: [frontend]
files:
  - frontend/package.json
  - frontend/vite.config.js
  - frontend/src/style.css
  - frontend/src/App.vue
  - frontend/src/views/HomeView.vue
---

# 前端 Tailwind CSS v4 接入与存量样式改写

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui` |
| 已启用 profile | frontend |
| 架构边界 | frontend 工程根（依赖/构建配置）+ 全局样式入口 + 存量两个组件的 utilities 化 |
| 结构分析 | design-system 前置：**已生成并确认**（`frontend/design-system/`，Tailwind 基准）；参考目录只读，运行时引入：无 |
| 完成条件 | `npm run build` 通过；页面视觉与改造前一致（白底卡片/蓝色按钮/同色板）；无任何指向 design-system/ 的 import |

## 需求与验收

- 用户目标：样式体系统一切到 Tailwind v4 utility-first，`@theme` 承载 token，消灭手写 BEM 全局类。
- 包含：安装 `tailwindcss` + `@tailwindcss/vite`；`style.css` 重写为 Tailwind 入口；`App.vue`/`HomeView.vue` 存量类改写为 utilities。
- 不包含：认证相关页面与逻辑（任务 07/08）。
- 验收：构建通过；dev 页面肉眼比对无回归（卡片圆角 10px、主色 #3370ff、健康检查与计数器功能不变）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 无接口；视觉输出不变（token 值来自 `frontend/design-system/tokens.md` 迁移映射） |
| 数据/Schema | 不适用 |
| 集成 | Tailwind v4 CSS-first：无 JS 配置文件，`@theme` 定义于 style.css；vite 插件 `@tailwindcss/vite` |
| 安全与质量 | 语义色全部走 `@theme` token（`bg-primary` 等），utilities 中不硬编码 hex |
| 依赖 | 无前置任务（可与后端并行）；任务 08 页面将直接使用本任务建立的 utilities 体系 |

## 实现步骤

1. `cd frontend && npm install -D tailwindcss @tailwindcss/vite`（v4 最新 4.x）；
2. `vite.config.js` 加 `tailwindcss()` 插件；
3. 重写 `src/style.css`：`@import 'tailwindcss'` + `@theme` token + base 层 body 样式；
4. `App.vue`、`HomeView.vue` 模板改用 utilities（script 逻辑不动）；
5. `npm run dev` 肉眼比对 + `npm run build` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `frontend/package.json` | 工程根 | 修改 | devDependencies 增 tailwindcss/@tailwindcss/vite |
| `frontend/vite.config.js` | 工程根 | 修改 | plugins 加 tailwindcss() |
| `frontend/src/style.css` | 全局样式 | 修改 | 重写为 Tailwind 入口 |
| `frontend/src/App.vue` | 根组件 | 修改 | 模板 utilities 化 |
| `frontend/src/views/HomeView.vue` | 页面 | 修改 | 模板 utilities 化 |

## 完整代码（供手动敲写）

### frontend/package.json（修改）

```json
{
  "name": "reprise-frontend",
  "private": true,
  "version": "0.0.0",
  "description": "reprise 全栈复现练习 - 前端基座（Vue 3 + Vite + JavaScript）",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.20.0",
    "pinia": "^4.0.3",
    "vue": "^3.5.41",
    "vue-router": "^4.6.4"
  },
  "devDependencies": {
    "@tailwindcss/vite": "^4.1.0",
    "@vitejs/plugin-vue": "^6.0.8",
    "tailwindcss": "^4.1.0",
    "vite": "^8.2.2"
  }
}
```

修改点说明：仅 devDependencies 增两项（实际版本以 `npm install -D` 安装结果为准，^4 自动取最新 4.x）。落点：devDependencies 段。

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

修改点说明：新增 import 与 `plugins` 数组中的 `tailwindcss()`；其余保持原样。

### frontend/src/style.css（修改）

```css
@import 'tailwindcss';

/*
 * 场景 01 起，样式体系为 Tailwind v4 utility-first。
 * @theme 值由原 :root CSS 变量整体迁移（色板不变），映射表见 frontend/design-system/tokens.md。
 */
@theme {
  --color-canvas: #f6f7f9;
  --color-ink: #1f2329;
  --color-ink-secondary: #646a73;
  --color-line: #e5e6eb;
  --color-primary: #3370ff;
  --color-primary-hover: #2b5fd9;
  --color-success: #34c724;
  --color-danger: #f54a45;
  --color-code-bg: #f0f1f3;
  --radius-card: 10px;
  --font-sans: 'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

@layer base {
  body {
    @apply bg-canvas font-sans text-ink antialiased;
  }
}
```

修改点说明：全量替换原文件。卡片背景用默认 `white`，按钮/小元素圆角用默认 `rounded-md`(6px)。

### frontend/src/App.vue（修改）

```vue
<script setup>
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <header class="flex items-baseline gap-3 border-b border-line bg-white px-8 py-4">
      <span class="text-xl font-bold tracking-wide">reprise</span>
      <span class="text-[13px] text-ink-secondary">经典前后端场景复现</span>
    </header>
    <main class="mx-auto w-full max-w-[960px] flex-1 px-6 py-8">
      <RouterView />
    </main>
  </div>
</template>
```

修改点说明：仅模板替换为 utilities（对应原 `.layout`/`.header`/`.brand`/`.tagline`/`.main`）。

### frontend/src/views/HomeView.vue（修改）

```vue
<script setup>
import { onMounted, ref } from 'vue'

import { getHealth } from '@/api/health'
import { useCounterStore } from '@/stores/counter'

const counter = useCounterStore()

const health = ref(null)
const healthError = ref('')
const loading = ref(false)

const checkHealth = async () => {
  loading.value = true
  healthError.value = ''
  try {
    health.value = await getHealth()
  } catch (error) {
    health.value = null
    healthError.value = error instanceof Error ? error.message : String(error)
  } finally {
    loading.value = false
  }
}

onMounted(checkHealth)
</script>

<template>
  <section class="mb-4 rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-3 text-[15px] font-semibold">后端健康检查</h2>
    <div class="flex flex-wrap items-center gap-3">
      <button
        class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="loading"
        @click="checkHealth"
      >
        {{ loading ? '检查中…' : '检查 /api/health' }}
      </button>
      <template v-if="health">
        <span class="inline-block h-2 w-2 rounded-full" :class="health.status === 'UP' ? 'bg-success' : 'bg-danger'" />
        <span>{{ health.status }}</span>
        <span class="text-[13px] text-ink-secondary">{{ health.time }}</span>
      </template>
      <span v-else-if="healthError" class="text-[13px] text-danger">{{ healthError }}</span>
      <span v-else class="text-[13px] text-ink-secondary">尚未检查</span>
    </div>
  </section>

  <section class="mb-4 rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-3 text-[15px] font-semibold">状态管理（Pinia）</h2>
    <div class="flex flex-wrap items-center gap-3">
      <button
        class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
        @click="counter.increment()"
      >
        count + 1
      </button>
      <span>count = <code class="rounded bg-code-bg px-1.5 py-0.5 text-[13px]">{{ counter.count }}</code></span>
    </div>
  </section>
</template>
```

修改点说明：script 逻辑不动；模板 `.card`/`.row`/`.muted`/`.dot`/`.error`/`code`/`button` 全部替换为 utilities。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 构建成功（Tailwind 参与编译） |
| manual | `npm run dev` 打开 http://localhost:5173 | 视觉与改造前一致，健康检查/计数器可用 |
| manual | 全局搜索 `design-system` | 前端源码无任何指向该目录的 import |

## 风险与阻塞

- 风险：Tailwind 4.x 与 Vite 8 版本兼容以安装时 npm 解析为准；若有 peer 冲突以官方文档推荐版本调整。
- 阻塞：无。
- 执行记录：2026-09-05 安装 tailwindcss@4.3.3 + @tailwindcss/vite@4.3.3；全部文件按代码块落盘；`npm run build` 成功（CSS 12.12 kB，utilities 已编译）；源码无 design-system 运行时引用。
