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
