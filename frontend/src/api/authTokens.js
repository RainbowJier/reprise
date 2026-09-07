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
