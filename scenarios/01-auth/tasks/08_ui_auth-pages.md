---
id: auth-jwt_08_ui_auth_pages
name: "登录/注册页面与首页用户区"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_06_ui_tailwind_setup, auth-jwt_07_ui_auth_infra]
profiles: [frontend]
files:
  - frontend/src/views/LoginView.vue
  - frontend/src/views/RegisterView.vue
  - frontend/src/views/HomeView.vue
  - frontend/src/router/index.js
---

# 登录/注册页面与首页用户区

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui` |
| 已启用 profile | frontend |
| 架构边界 | frontend views（新增两页 + HomeView 叠加用户区）；router 替换占位组件 |
| 结构分析 | design-system 前置：已生成并确认；样式全部使用任务 06 的 utilities + `@theme` token，禁止 hex 硬编码与新增全局类 |
| 完成条件 | `npm run build` 通过；后端可用时注册/登录/登出/受保护跳转全流程可走通（联调细节在任务 09） |

## 需求与验收

- 用户目标：用户能注册、登录、在首页看到自己并登出。
- 包含：LoginView（含 demo 账号提示）、RegisterView（昵称可选 + 密码确认本地校验）、HomeView 用户卡与登出、router 换真实页面。
- 不包含：找回密码、记住我、第三方登录（设计非目标）。
- 验收：登录成功按 `redirect` 回跳（默认 `/`）；错误信息红字展示；登出后回登录页；刷新页面登录态保持（token 持久化）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 调用任务 07 的 store（login/register/fetchUser/logout）；表单错误态用 `border-danger`/`text-danger` token |
| 数据/Schema | 不适用 |
| 集成 | 不适用 |
| 安全与质量 | 提交按钮防重复（loading）；密码输入 `autocomplete` 语义正确 |
| 依赖 | 06（utilities 体系）、07（store/守卫/占位替换点） |

## 实现步骤

1. 新建 `LoginView.vue`、`RegisterView.vue`；
2. `HomeView.vue` 叠加「当前用户」卡片与登出（基于任务 06 的 utilities 版本）；
3. `router/index.js` 删除占位组件，换懒加载导入；
4. `npm run build` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `frontend/src/views/LoginView.vue` | views | 新增 | 登录页 |
| `frontend/src/views/RegisterView.vue` | views | 新增 | 注册页 |
| `frontend/src/views/HomeView.vue` | views | 修改 | 增加用户区 |
| `frontend/src/router/index.js` | router | 修改 | 占位换真实页面 |

## 完整代码（供手动敲写）

### frontend/src/views/LoginView.vue（新增）

```vue
<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

const submit = async () => {
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login({ ...form })
    router.push(route.query.redirect || '/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="mx-auto mb-4 w-full max-w-sm rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-4 text-[15px] font-semibold">登录</h2>
    <form class="flex flex-col gap-4" @submit.prevent="submit">
      <label class="block">
        <span class="mb-1 block text-sm text-ink-secondary">用户名</span>
        <input
          v-model.trim="form.username"
          class="w-full rounded-md border border-line px-3 py-2 text-sm focus:border-primary focus:outline-none"
          :class="error ? 'border-danger' : ''"
          placeholder="demo"
          autocomplete="username"
        >
      </label>
      <label class="block">
        <span class="mb-1 block text-sm text-ink-secondary">密码</span>
        <input
          v-model.trim="form.password"
          type="password"
          class="w-full rounded-md border border-line px-3 py-2 text-sm focus:border-primary focus:outline-none"
          :class="error ? 'border-danger' : ''"
          placeholder="demo123456"
          autocomplete="current-password"
        >
      </label>
      <p v-if="error" class="text-[13px] text-danger">{{ error }}</p>
      <div class="flex items-center gap-3">
        <button
          type="submit"
          class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
        >
          {{ loading ? '登录中…' : '登录' }}
        </button>
        <RouterLink to="/register" class="text-sm text-primary hover:underline">没有账号？去注册</RouterLink>
      </div>
      <p class="text-[13px] text-ink-secondary">演示账号：demo / demo123456</p>
    </form>
  </section>
</template>
```

### frontend/src/views/RegisterView.vue（新增）

```vue
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })
const error = ref('')
const loading = ref(false)

const submit = async () => {
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  if (form.password !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  error.value = ''
  try {
    // 注册即登录：成功后直接进入首页
    await auth.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
    })
    router.push('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="mx-auto mb-4 w-full max-w-sm rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-4 text-[15px] font-semibold">注册</h2>
    <form class="flex flex-col gap-4" @submit.prevent="submit">
      <label class="block">
        <span class="mb-1 block text-sm text-ink-secondary">用户名（4-32 位字母数字下划线）</span>
        <input
          v-model.trim="form.username"
          class="w-full rounded-md border border-line px-3 py-2 text-sm focus:border-primary focus:outline-none"
          :class="error ? 'border-danger' : ''"
          placeholder="zhang_san"
          autocomplete="username"
        >
      </label>
      <label class="block">
        <span class="mb-1 block text-sm text-ink-secondary">密码（6-64 位）</span>
        <input
          v-model.trim="form.password"
          type="password"
          class="w-full rounded-md border border-line px-3 py-2 text-sm focus:border-primary focus:outline-none"
          :class="error ? 'border-danger' : ''"
          placeholder="至少 6 位"
          autocomplete="new-password"
        >
      </label>
      <label class="block">
        <span class="mb-1 block text-sm text-ink-secondary">确认密码</span>
        <input
          v-model.trim="form.confirmPassword"
          type="password"
          class="w-full rounded-md border border-line px-3 py-2 text-sm focus:border-primary focus:outline-none"
          :class="error ? 'border-danger' : ''"
          autocomplete="new-password"
        >
      </label>
      <label class="block">
        <span class="mb-1 block text-sm text-ink-secondary">昵称（可选）</span>
        <input
          v-model.trim="form.nickname"
          class="w-full rounded-md border border-line px-3 py-2 text-sm focus:border-primary focus:outline-none"
          placeholder="留空则同用户名"
        >
      </label>
      <p v-if="error" class="text-[13px] text-danger">{{ error }}</p>
      <div class="flex items-center gap-3">
        <button
          type="submit"
          class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
        >
          {{ loading ? '注册中…' : '注册' }}
        </button>
        <RouterLink to="/login" class="text-sm text-primary hover:underline">已有账号？去登录</RouterLink>
      </div>
    </form>
  </section>
</template>
```

### frontend/src/views/HomeView.vue（修改）

```vue
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getHealth } from '@/api/health'
import { useCounterStore } from '@/stores/counter'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const counter = useCounterStore()
const auth = useAuthStore()

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

const logout = () => {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  checkHealth()
  // token 恢复的登录态在此拉取最新资料（失败由拦截器统一处理）
  if (auth.isLoggedIn) {
    auth.fetchUser().catch(() => {})
  }
})
</script>

<template>
  <section class="mb-4 rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-3 text-[15px] font-semibold">当前用户</h2>
    <div class="flex flex-wrap items-center gap-3">
      <template v-if="auth.user">
        <span class="text-sm font-medium">{{ auth.user.nickname }}</span>
        <span class="text-[13px] text-ink-secondary">@{{ auth.user.username }}</span>
        <button
          class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
          @click="logout"
        >
          登出
        </button>
      </template>
      <span v-else class="text-[13px] text-ink-secondary">未登录</span>
    </div>
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

修改点说明：基于任务 06 的 utilities 版本，script 增加 router/auth 导入、logout 与 onMounted 拉取用户；模板顶部新增「当前用户」卡片，其余不变。

### frontend/src/router/index.js（修改）

```javascript
import { createRouter, createWebHistory } from 'vue-router'

import { isLoggedIn } from '@/api/authTokens'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true },
    },
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
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
```

修改点说明：删除任务 07 的占位组件定义，三个路由统一懒加载；守卫逻辑不变。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 构建成功 |
| manual | 后端可用时：注册 → 首页显示昵称 → 登出 → 被守卫拦回登录页 → demo 账号登录 → redirect 回首页 | 全流程通过（细化清单见任务 09） |

## 风险与阻塞

- 风险：无后端时页面只能验证构建与跳转逻辑；表单校验以后端 417 文案为准（本地仅做非空与密码一致性）。
- 阻塞：无。
- 执行记录：2026-09-05 落盘 LoginView/RegisterView/HomeView（用户区版）/router（最终懒加载版）；`npm run build` 成功（三个页面 chunk + auth chunk 产出）；浏览器端到端流程在任务 09 验证。
