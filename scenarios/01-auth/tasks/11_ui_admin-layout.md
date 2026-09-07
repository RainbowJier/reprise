---
id: auth-jwt_11_ui_admin_layout
name: "RuoYi 风格管理端布局（左侧场景菜单 + 右侧详情）"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_08_ui_auth_pages]
profiles: [frontend]
files:
  - frontend/src/config/scenarios.js
  - frontend/src/layouts/AdminLayout.vue
  - frontend/src/views/scenario/AuthView.vue
  - frontend/src/views/HomeView.vue
  - frontend/src/router/index.js
  - frontend/src/App.vue
  - frontend/src/views/LoginView.vue
  - frontend/src/views/RegisterView.vue
  - frontend/src/style.css
  - frontend/design-system/tokens.md
  - frontend/design-system/design-system.md
  - AGENTS.md
---

# RuoYi 风格管理端布局（左侧场景菜单 + 右侧详情）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui`（增量：场景 01 完成后的基座布局改造） |
| 已启用 profile | frontend |
| 架构边界 | frontend 全局布局与路由结构；不改后端与认证逻辑 |
| 结构分析 | design-system 前置：已生成并确认；RuoYi 布局为用户指定的参照；菜单数据取前端静态配置（与 README 场景索引一致），后端动态下发留作扩展 |
| 完成条件 | `npm run build` 绿；浏览器验证登录后进入管理端布局，侧栏菜单与路由联动，规划中场景置灰 |

## 需求与验收

- 用户目标：前端呈 RuoYi 管理端形态——左侧每种场景一个菜单，右侧详情页面。
- 包含：AdminLayout（侧栏+顶栏+内容区）、场景静态配置、路由重构、HomeView 改造为场景 01 详情页（AuthView）、登录/注册独立全屏页、侧栏 token、design-system 与 AGENTS 同步。
- 不包含：后端菜单/权限下发、多标签页（tags-view）、侧栏折叠、移动端适配。
- 验收：未登录访问任意场景页跳登录；登录后见侧栏（01 高亮可用、02-10 置灰「规划中」）；顶栏显示用户与登出下拉；`/` 重定向 `/scenario/01-auth`。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 布局 UI 变更；认证流程/guard/拦截器逻辑不动（guard 挂在布局父路由） |
| 数据/Schema | 不适用 |
| 集成 | 不适用 |
| 安全与质量 | requiresAuth 挂父路由（子路由继承）；规划中菜单不可点击 |
| 依赖 | 08（页面与设施，已完成）；复用 authTokens/store/http |

## 实现步骤

1. 新建 `config/scenarios.js`（与 README 场景索引对齐，enabled 标记是否已实现）；
2. 新建 `layouts/AdminLayout.vue`（侧栏菜单 + 顶栏面包屑/用户下拉 + RouterView）；
3. HomeView 改造为 `views/scenario/AuthView.vue`（去用户卡、加场景简介卡），删除 HomeView；
4. 路由重构为布局嵌套 + 兜底重定向；App.vue 简化为纯 RouterView；
5. 登录/注册页独立全屏（垂直留白微调）；style.css 增侧栏 token；
6. 同步 design-system（token + AdminLayout 组件登记）与 AGENTS.md 前端布局约定；
7. `npm run build` + 浏览器验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `frontend/src/config/scenarios.js` | config | 新增 | 场景菜单静态配置 |
| `frontend/src/layouts/AdminLayout.vue` | layouts | 新增 | 管理端布局骨架 |
| `frontend/src/views/scenario/AuthView.vue` | views | 新增 | 场景 01 详情页（原 HomeView 改造） |
| `frontend/src/views/HomeView.vue` | views | 删除 | 内容迁移至 AuthView |
| `frontend/src/router/index.js` | router | 修改 | 布局嵌套 + 兜底 |
| `frontend/src/App.vue` | 根组件 | 修改 | 简化为 RouterView |
| `frontend/src/views/LoginView.vue` / `RegisterView.vue` | views | 修改 | 独立页留白 |
| `frontend/src/style.css` | 全局样式 | 修改 | 侧栏 token |
| `frontend/design-system/*` / `AGENTS.md` | 文档 | 修改 | 同步登记 |

## 完整代码（供手动敲写）

### frontend/src/config/scenarios.js（新增）

```javascript
// 场景菜单清单：与 README 场景索引保持一致
// enabled = 已在本仓库实现（路由可达）；未实现的场景在侧栏置灰展示为「规划中」
// 扩展点：接入后端菜单/权限下发后，本文件可改为接口数据驱动
export const scenarios = [
  { id: '01-auth', no: '01', name: '用户登录与认证', desc: 'JWT 双 token 无感续期', path: '/scenario/01-auth', enabled: true },
  { id: '02-short-link', no: '02', name: '短链接服务', path: '/scenario/02-short-link', enabled: false },
  { id: '03-flash-sale', no: '03', name: '秒杀抢购', path: '/scenario/03-flash-sale', enabled: false },
  { id: '04-feed', no: '04', name: 'Feed 流', path: '/scenario/04-feed', enabled: false },
  { id: '05-im', no: '05', name: '即时通讯', path: '/scenario/05-im', enabled: false },
  { id: '06-upload', no: '06', name: '文件上传', path: '/scenario/06-upload', enabled: false },
  { id: '07-payment', no: '07', name: '支付回调对账', path: '/scenario/07-payment', enabled: false },
  { id: '08-rbac', no: '08', name: 'RBAC 权限', path: '/scenario/08-rbac', enabled: false },
  { id: '09-distributed-lock', no: '09', name: '分布式锁', path: '/scenario/09-distributed-lock', enabled: false },
  { id: '10-caching', no: '10', name: '多级缓存', path: '/scenario/10-caching', enabled: false },
]
```

### frontend/src/layouts/AdminLayout.vue（新增）

```vue
<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { scenarios } from '@/config/scenarios'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const userMenuOpen = ref(false)

const currentScenario = computed(() => scenarios.find((s) => s.path === route.path))

// 头像取昵称首字符（中文昵称即单字）
const avatarChar = computed(() => (auth.user?.nickname || '?').slice(0, 1))

const logout = () => {
  userMenuOpen.value = false
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="flex min-h-screen">
    <!-- 左侧：品牌 + 场景菜单（RuoYi 深色侧栏） -->
    <aside class="flex w-56 shrink-0 flex-col bg-sidebar">
      <div class="flex h-14 shrink-0 items-center border-b border-white/10 px-5">
        <span class="text-base font-bold text-white">reprise</span>
        <span class="ml-2 text-xs text-sidebar-text/70">场景复现</span>
      </div>
      <nav class="flex-1 overflow-y-auto py-2">
        <component
          :is="s.enabled ? 'RouterLink' : 'div'"
          v-for="s in scenarios"
          :key="s.id"
          :to="s.enabled ? s.path : undefined"
          class="flex items-center gap-2.5 border-l-4 border-transparent px-4 py-2.5 text-sm"
          :class="s.path === route.path
            ? 'border-primary bg-sidebar-active text-white'
            : s.enabled
              ? 'text-sidebar-text hover:bg-sidebar-hover hover:text-white'
              : 'cursor-not-allowed text-sidebar-text/40'"
          :title="s.enabled ? s.name : `${s.name}（规划中）`"
        >
          <span class="rounded bg-white/10 px-1.5 py-0.5 font-mono text-[10px]">{{ s.no }}</span>
          <span>{{ s.name }}</span>
          <span
            v-if="!s.enabled"
            class="ml-auto rounded-full border border-sidebar-text/20 px-2 py-0.5 text-[10px]"
          >规划中</span>
        </component>
      </nav>
    </aside>

    <!-- 右侧：顶栏 + 内容区 -->
    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex h-14 shrink-0 items-center justify-between border-b border-line bg-white px-6">
        <div class="flex items-center gap-2 text-sm">
          <span class="text-ink-secondary">场景复现</span>
          <template v-if="currentScenario">
            <span class="text-ink-secondary">/</span>
            <span>{{ currentScenario.no }} {{ currentScenario.name }}</span>
          </template>
        </div>

        <div v-if="auth.user" class="relative">
          <button
            class="flex items-center gap-2 rounded-md px-3 py-1.5 text-sm hover:bg-canvas"
            @click="userMenuOpen = !userMenuOpen"
          >
            <span class="inline-block h-6 w-6 rounded-full bg-primary text-center text-xs leading-6 text-white">{{ avatarChar }}</span>
            <span>{{ auth.user.nickname }}</span>
            <span class="text-[10px] text-ink-secondary">▾</span>
          </button>
          <!-- 点击空白处关闭下拉 -->
          <div v-if="userMenuOpen" class="fixed inset-0 z-10" @click="userMenuOpen = false" />
          <div
            v-if="userMenuOpen"
            class="absolute right-0 top-full z-20 mt-1 w-36 rounded-md border border-line bg-white py-1 shadow-sm"
          >
            <div class="px-3 py-1.5 text-xs text-ink-secondary">@{{ auth.user.username }}</div>
            <button
              class="block w-full px-3 py-1.5 text-left text-sm text-danger hover:bg-canvas"
              @click="logout"
            >
              退出登录
            </button>
          </div>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
```

### frontend/src/views/scenario/AuthView.vue（新增，承接原 HomeView）

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
    <h2 class="mb-2 text-[15px] font-semibold">场景 01 · 用户登录与认证</h2>
    <p class="text-[13px] leading-6 text-ink-secondary">
      JWT 双 token 无感续期：注册 → 登录 → 携带 Bearer 访问受保护接口 → access 过期静默刷新 → 登出。
      设计与取舍见 scenarios/01-auth/NOTES.md。
    </p>
  </section>

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

### frontend/src/router/index.js（修改）

```javascript
import { createRouter, createWebHistory } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { isLoggedIn } from '@/api/authTokens'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      // 管理端布局：左侧场景菜单 + 右侧详情，登录后才可进入
      path: '/',
      component: AdminLayout,
      redirect: '/scenario/01-auth',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'scenario/01-auth',
          name: 'scenario-01-auth',
          component: () => import('@/views/scenario/AuthView.vue'),
          meta: { title: '用户登录与认证' },
        },
        // 后续场景：在此追加子路由，并在 config/scenarios.js 启用对应菜单
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
```

修改点说明：requiresAuth 挂布局父路由（vue-router 4 的 `to.meta` 会合并匹配链上的 meta，子路由继承鉴权）；`/` 重定向到首个已实现场景；通配兜底回首页。

### frontend/src/App.vue（修改）

```vue
<script setup>
</script>

<template>
  <RouterView />
</template>
```

修改点说明：页面骨架（header/main）下沉到 AdminLayout 与独立登录页，根组件只留路由出口。

### frontend/src/views/LoginView.vue（修改）

外层 section 类由 `mx-auto mb-4 w-full max-w-sm rounded-card border border-line bg-white px-6 py-5` 调整为 `mx-auto mt-16 w-full max-w-sm rounded-card border border-line bg-white px-6 py-5`（独立全屏页垂直留白）。RegisterView 同样把 `mb-4` 换成 `mt-16`。

### frontend/src/style.css（修改）

`@theme` 块追加侧栏 token：

```css
  /* RuoYi 风格管理端侧栏（场景 01 后基座布局） */
  --color-sidebar: #304156;
  --color-sidebar-hover: #263445;
  --color-sidebar-active: #1f2d3d;
  --color-sidebar-text: #bfcbd9;
```

### frontend/design-system/tokens.md（修改）

颜色表追加四行（类型「已有事实」，说明「管理端侧栏（RuoYi 参照）」）：`--color-sidebar #304156`、`--color-sidebar-hover #263445`、`--color-sidebar-active #1f2d3d`、`--color-sidebar-text #bfcbd9`。

### frontend/design-system/design-system.md（修改）

- 项目事实「组件总数：0」改为「1」；
- 组件总表登记 `AdminLayout`（布局骨架 | `src/layouts/AdminLayout.vue` | 自有 | 已确认）；
- 组件条目区补 AdminLayout 详情：Props 无、Emits 无、Slots default（内容区）、何时用=登录后的管理端页面、何时不用=登录/注册等独立全屏页、辨析=无等价组件。

### AGENTS.md（修改）

前端约定追加：管理端布局说明（AdminLayout + scenarios.js 注册新场景的方式）。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 构建成功 |
| manual | 登录后查看布局 | 侧栏 01 高亮、02-10 置灰「规划中」；顶栏昵称下拉可登出 |
| manual | 未登录直接访问 `/scenario/01-auth` | 跳 `/login?redirect=/scenario/01-auth` |
| manual | 登录后访问 `/` | 重定向 `/scenario/01-auth` |

## 风险与阻塞

- 风险：HomeView 删除后若有残留引用会在构建期暴露；guard 依赖 meta 合并行为（vue-router 4 标准行为）。
- 阻塞：无。
- 执行记录：2026-09-05 全部文件按代码块落盘（含 HomeView 删除、design-system 与 AGENTS 同步）；`npm run build` 成功；浏览器验证通过——登录后进入 `/scenario/01-auth` 管理端布局，侧栏 01 高亮可用、02-10 置灰「规划中」，顶栏头像下拉可登出，登出回 `/login`，未登录访问场景页被拦到 `/login?redirect=/scenario/01-auth`；截图确认 RuoYi 视觉形态（深色侧栏 + 面包屑 + 右侧卡片详情）。
