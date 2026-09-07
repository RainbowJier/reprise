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
  <section class="mx-auto mt-16 w-full max-w-sm animate-fade-up rounded-card border border-line bg-white px-6 py-5">
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
      <p v-if="error" :key="error" class="animate-shake text-[13px] text-danger">{{ error }}</p>
      <div class="flex items-center gap-3">
        <button
          type="submit"
          class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
        >
          <span v-if="loading" class="mr-1 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white align-[-2px]" />
          {{ loading ? '登录中…' : '登录' }}
        </button>
        <RouterLink to="/register" class="text-sm text-primary hover:underline">没有账号？去注册</RouterLink>
      </div>
      <p class="text-[13px] text-ink-secondary">演示账号：demo / demo123456</p>
    </form>
  </section>
</template>
