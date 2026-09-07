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
  <section class="mx-auto mt-16 w-full max-w-sm animate-fade-up rounded-card border border-line bg-white px-6 py-5">
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
      <p v-if="error" :key="error" class="animate-shake text-[13px] text-danger">{{ error }}</p>
      <div class="flex items-center gap-3">
        <button
          type="submit"
          class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
        >
          <span v-if="loading" class="mr-1 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white align-[-2px]" />
          {{ loading ? '注册中…' : '注册' }}
        </button>
        <RouterLink to="/login" class="text-sm text-primary hover:underline">已有账号？去登录</RouterLink>
      </div>
    </form>
  </section>
</template>
