<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import AuthShell from '@/components/AuthShell.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)
const showPassword = ref(false)
const fillDemo = () => { form.username = 'demo'; form.password = 'demo123456'; error.value = '' }
const submit = async () => {
  if (!form.username || !form.password) { error.value = '请输入用户名和密码'; return }
  loading.value = true
  error.value = ''
  try {
    await auth.login({ ...form })
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
  <AuthShell title="回到场景库" subtitle="登录后，继续阅读文档与探索交互演示。" eyebrow="WELCOME BACK">
    <form class="space-y-5" @submit.prevent="submit">
      <label class="block"><span class="mb-2 block text-xs font-medium">用户名</span><input v-model.trim="form.username" required class="w-full rounded-lg border border-line bg-canvas/60 px-3 py-3 text-sm focus:border-primary focus:outline-none" placeholder="请输入用户名" autocomplete="username" :aria-invalid="Boolean(error)" :aria-describedby="error ? 'login-error' : undefined" /></label>
      <div><label for="login-password" class="mb-2 block text-xs font-medium">密码</label><div class="relative"><input id="login-password" v-model="form.password" required :type="showPassword ? 'text' : 'password'" class="w-full rounded-lg border border-line bg-canvas/60 py-3 pl-3 pr-11 text-sm focus:border-primary focus:outline-none" placeholder="请输入密码" autocomplete="current-password" :aria-invalid="Boolean(error)" :aria-describedby="error ? 'login-error' : undefined" /><button type="button" class="absolute right-2 top-2 rounded p-2 text-ink-secondary" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :aria-pressed="showPassword" @click="showPassword = !showPassword"><AppIcon name="eye" /></button></div></div>
      <p v-if="error" id="login-error" role="alert" class="rounded-lg bg-danger/5 px-3 py-2 text-xs leading-5 text-danger">{{ error }}</p>
      <button class="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-3 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-60" :disabled="loading"><span v-if="loading" class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />{{ loading ? '登录中…' : '登录并开始探索' }}<AppIcon v-if="!loading" name="arrow" /></button>
    </form>
    <div class="mt-5 rounded-lg border border-line bg-canvas p-3"><div class="flex items-center justify-between gap-2 text-xs"><span class="text-ink-secondary">快速体验</span><button type="button" class="font-medium text-primary hover:underline" :disabled="loading" @click="fillDemo">填入演示账号</button></div><p class="mt-2 font-mono text-[11px] text-ink-secondary">demo / demo123456</p></div>
    <p class="mt-6 text-center text-xs text-ink-secondary">还没有账号？<RouterLink :to="{ path: '/register', query: route.query }" class="ml-1 font-medium text-primary hover:underline">创建账号</RouterLink></p>
  </AuthShell>
</template>
