---
id: auth-jwt_07_ui_auth_infra
name: "前端认证设施（token 存储/拦截器刷新/守卫/Store）"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: []
profiles: [frontend, api]
files:
  - frontend/src/api/authTokens.js
  - frontend/src/api/http.js
  - frontend/src/api/auth.js
  - frontend/src/api/user.js
  - frontend/src/stores/auth.js
  - frontend/src/router/index.js
---

# 前端认证设施（token 存储/拦截器刷新/守卫/Store）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui` |
| 已启用 profile | frontend / api |
| 架构边界 | frontend src/api（token 存储、axios 拦截器、接口模块）、stores（auth store）、router（守卫） |
| 结构分析 | 契约来源=设计文档「六、接口设计」（已冻结），不依赖后端任务完成；依赖关系设计：token 存储独立成 `authTokens.js` 纯模块，http.js 与 store 都只依赖它，避免 http↔store 循环引用 |
| 完成条件 | `npm run build` 通过；守卫与拦截器逻辑就绪（页面在任务 08） |

## 需求与验收

- 用户目标：登录态的存取、携带、无感续期与路由保护在设施层闭环。
- 包含：localStorage token 读写、请求拦截器注入 Bearer、401 单飞刷新 + 并发重放 + 失败清空跳登录、auth store、路由守卫。
- 不包含：登录/注册页面 UI（任务 08）、HomeView 改造（任务 08）。
- 验收：构建通过；代码审查确认无循环依赖（http → authTokens ← store；router → authTokens）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 消费契约：login/register 返回 TokenResp、业务 401 走 HTTP 200 + body code=401 |
| 数据/Schema | localStorage key：`reprise.auth.tokens` |
| 集成 | 刷新请求走独立裸 axios 实例（`refreshHttp`），绕开自身拦截器避免 401 递归 |
| 安全与质量 | `__retried` 标记防重放死循环；登录接口的 401（密码错）不触发刷新（无 refreshToken 时直接走通用 reject） |
| 依赖 | 契约已冻结（设计文档），无后端任务依赖；页面任务 08 依赖本任务 |

## 实现步骤

1. 新建 `api/authTokens.js`（纯 localStorage 读写）；
2. 改造 `api/http.js`（请求/响应拦截器 + 单飞刷新 + 重放）；
3. 新建 `api/auth.js`、`api/user.js`；
4. 新建 `stores/auth.js`；
5. 改造 `router/index.js`（新路由占位 + 守卫；页面组件由任务 08 落盘，本任务先用内联占位组件保证可构建）；
6. `npm run build` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `frontend/src/api/authTokens.js` | api | 新增 | token 存储纯模块 |
| `frontend/src/api/http.js` | api | 修改 | 拦截器与刷新逻辑 |
| `frontend/src/api/auth.js` | api | 新增 | 认证接口 |
| `frontend/src/api/user.js` | api | 新增 | 用户接口 |
| `frontend/src/stores/auth.js` | stores | 新增 | 认证状态 |
| `frontend/src/router/index.js` | router | 修改 | 路由与守卫 |

## 完整代码（供手动敲写）

### frontend/src/api/authTokens.js（新增）

```javascript
// token 对的 localStorage 存取（纯模块，无任何依赖）
// http 拦截器与 auth store 共用，避免二者互相引用

const TOKENS_KEY = 'reprise.auth.tokens'

export function getTokens() {
  try {
    return JSON.parse(localStorage.getItem(TOKENS_KEY)) || null
  } catch {
    return null
  }
}

export function setTokens(tokens) {
  localStorage.setItem(TOKENS_KEY, JSON.stringify(tokens))
}

export function clearTokens() {
  localStorage.removeItem(TOKENS_KEY)
}

export function getAccessToken() {
  return getTokens()?.accessToken ?? ''
}

export function getRefreshToken() {
  return getTokens()?.refreshToken ?? ''
}

export function isLoggedIn() {
  return Boolean(getAccessToken())
}
```

### frontend/src/api/http.js（修改）

```javascript
import axios from 'axios'

import router from '@/router'

import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './authTokens'

const CODE_SUCCESS = 200
const CODE_UNAUTHORIZED = 401

export const http = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

// 刷新专用裸实例：绕开自身拦截器，避免 401 递归刷新
const refreshHttp = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

// 单飞刷新：并发 401 共享同一次 refresh 请求
let refreshing = null

async function refreshTokens() {
  if (!refreshing) {
    refreshing = refreshHttp
      .post('/auth/refresh', { refreshToken: getRefreshToken() })
      .then((response) => {
        const body = response.data
        if (body?.code !== CODE_SUCCESS || !body.data?.accessToken) {
          throw new Error(body?.msg || '刷新登录态失败')
        }
        setTokens(body.data)
        return body.data
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

function gotoLogin() {
  clearTokens()
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  async (response) => {
    const body = response.data

    // 登录态过期：静默刷新后重放原请求（__retried 防死循环；未登录时无 refreshToken，直接走通用失败）
    if (
      body?.code === CODE_UNAUTHORIZED &&
      !response.config.__retried &&
      getRefreshToken()
    ) {
      try {
        await refreshTokens()
        response.config.__retried = true
        response.config.headers.Authorization = `Bearer ${getAccessToken()}`
        return http.request(response.config)
      } catch {
        gotoLogin()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
    }

    if (body && typeof body.code === 'number' && body.code !== CODE_SUCCESS) {
      return Promise.reject(new Error(body.msg || `请求失败（code=${body.code}）`))
    }
    return response
  },
  (error) => {
    const msg = error.response?.data?.msg ?? error.message ?? '网络错误'
    return Promise.reject(new Error(msg))
  },
)

export async function get(url, config) {
  const { data } = await http.get(url, config)
  return data.data
}

export async function post(url, body, config) {
  const { data } = await http.post(url, body, config)
  return data.data
}
```

修改点说明：保留原 `get`/`post` 解包行为；新增请求拦截器、401 刷新分支与 `refreshHttp`。`router` 导入无循环（router/index.js 只依赖 authTokens）。

### frontend/src/api/auth.js（新增）

```javascript
import { post } from './http'

export const register = (data) => post('/auth/register', data)

export const login = (data) => post('/auth/login', data)
```

### frontend/src/api/user.js（新增）

```javascript
import { get } from './http'

export const getMe = () => get('/user/me')
```

### frontend/src/stores/auth.js（新增）

```javascript
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { login as loginApi, register as registerApi } from '@/api/auth'
import { getMe } from '@/api/user'
import { clearTokens, getTokens, setTokens } from '@/api/authTokens'

export const useAuthStore = defineStore('auth', () => {
  const initial = getTokens()
  const user = ref(
    initial
      ? { id: initial.userId, username: initial.username, nickname: initial.nickname }
      : null,
  )

  const isLoggedIn = computed(() => Boolean(user.value))

  function applyTokens(tokens) {
    setTokens(tokens)
    user.value = { id: tokens.userId, username: tokens.username, nickname: tokens.nickname }
  }

  async function login(payload) {
    applyTokens(await loginApi(payload))
  }

  async function register(payload) {
    applyTokens(await registerApi(payload))
  }

  async function fetchUser() {
    user.value = await getMe()
    return user.value
  }

  function logout() {
    clearTokens()
    user.value = null
  }

  return { user, isLoggedIn, login, register, fetchUser, logout }
})
```

### frontend/src/router/index.js（修改）

```javascript
import { createRouter, createWebHistory } from 'vue-router'

import { isLoggedIn } from '@/api/authTokens'

// 占位页面：任务 08 落盘 LoginView/RegisterView 后替换为懒加载导入
const LoginPlaceholder = { template: '<section class="mb-4 rounded-card border border-line bg-white px-6 py-5"><h2 class="mb-3 text-[15px] font-semibold">登录页建设中…</h2></section>' }
const RegisterPlaceholder = { template: '<section class="mb-4 rounded-card border border-line bg-white px-6 py-5"><h2 class="mb-3 text-[15px] font-semibold">注册页建设中…</h2></section>' }

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
      component: LoginPlaceholder,
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterPlaceholder,
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

修改点说明：`/` 增加 `meta.requiresAuth`；本任务以占位组件保证可构建可运行，任务 08 替换为真实页面（替换点已在代码注释中标明）。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 构建成功 |
| manual | dev 下未登录访问 `/` | 被守卫重定向到 `/login?redirect=/` |

## 风险与阻塞

- 风险：后端未就绪时 401 刷新请求会失败——属预期（任务 08/09 联调验证）；占位组件依赖 Tailwind utilities 已存在（任务 06 先行）。
- 阻塞：无。
- 执行记录：2026-09-05 落盘 6 个文件（与任务 08 连续执行，router 直接落最终懒加载版，占位组件未经过渡落地）；`npm run build` 成功。

修订：1 - 与任务 08 合并连续执行，router/index.js 直接采用任务 08 的最终版（占位组件省略），依赖关系不受影响。
