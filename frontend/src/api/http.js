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
