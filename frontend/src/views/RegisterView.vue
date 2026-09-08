<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import AuthShell from '@/components/AuthShell.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })
const error = ref('')
const loading = ref(false)
const showPassword = ref(false)
const submit = async () => {
  if (!/^[A-Za-z0-9_]{4,32}$/.test(form.username)) { error.value = '用户名需为 4–32 位字母、数字或下划线'; return }
  if (form.password.length < 6 || form.password.length > 64) { error.value = '密码长度需为 6–64 位'; return }
  if (form.password !== form.confirmPassword) { error.value = '两次输入的密码不一致'; return }
  loading.value = true
  error.value = ''
  try {
    await auth.register({ username: form.username, password: form.password, nickname: form.nickname || undefined })
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="开始你的探索" subtitle="创建账号，体验从原理到实现的完整过程。" eyebrow="CREATE YOUR ACCOUNT">
    <form class="space-y-4" @submit.prevent="submit">
      <label class="block"><span class="mb-2 block text-xs font-medium">用户名</span><input v-model.trim="form.username" required minlength="4" maxlength="32" pattern="[A-Za-z0-9_]{4,32}" class="w-full rounded-lg border border-line bg-canvas/60 px-3 py-2.5 text-sm focus:border-primary focus:outline-none" placeholder="4–32 位字母、数字、下划线" autocomplete="username" /></label>
      <div><label for="register-password" class="mb-2 block text-xs font-medium">密码</label><div class="relative"><input id="register-password" v-model="form.password" required minlength="6" maxlength="64" :type="showPassword ? 'text' : 'password'" class="w-full rounded-lg border border-line bg-canvas/60 py-2.5 pl-3 pr-11 text-sm focus:border-primary focus:outline-none" placeholder="6–64 位密码" autocomplete="new-password" /><button type="button" class="absolute right-2 top-1.5 rounded p-2 text-ink-secondary" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :aria-pressed="showPassword" @click="showPassword = !showPassword"><AppIcon name="eye" /></button></div></div>
      <label class="block"><span class="mb-2 block text-xs font-medium">确认密码</span><input v-model="form.confirmPassword" required :type="showPassword ? 'text' : 'password'" class="w-full rounded-lg border border-line bg-canvas/60 px-3 py-2.5 text-sm focus:border-primary focus:outline-none" placeholder="再次输入密码" autocomplete="new-password" /></label>
      <label class="block"><span class="mb-2 block text-xs font-medium">昵称 <span class="font-normal text-ink-secondary">/ 可选</span></span><input v-model.trim="form.nickname" class="w-full rounded-lg border border-line bg-canvas/60 px-3 py-2.5 text-sm focus:border-primary focus:outline-none" placeholder="希望我们如何称呼你" autocomplete="nickname" /></label>
      <p v-if="error" role="alert" class="rounded-lg bg-danger/5 px-3 py-2 text-xs leading-5 text-danger">{{ error }}</p>
      <button class="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-3 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-60" :disabled="loading"><span v-if="loading" class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />{{ loading ? '创建中…' : '创建账号并进入' }}<AppIcon v-if="!loading" name="arrow" /></button>
    </form>
    <p class="mt-5 text-center text-xs text-ink-secondary">已有账号？<RouterLink :to="{ path: '/login', query: route.query }" class="ml-1 font-medium text-primary hover:underline">返回登录</RouterLink></p>
  </AuthShell>
</template>
