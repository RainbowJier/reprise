<script setup>
import { onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { getHealth } from '@/api/health'
import { useCounterStore } from '@/stores/counter'
import { useAuthStore } from '@/stores/auth'

const counter = useCounterStore()
const auth = useAuthStore()
const health = ref(null)
const healthError = ref('')
const loading = ref(false)
const profile = ref(null)
const profileError = ref('')
const checkingProfile = ref(false)
const checkHealth = async () => {
  loading.value = true
  healthError.value = ''
  try { health.value = await getHealth() }
  catch (error) { health.value = null; healthError.value = error instanceof Error ? error.message : String(error) }
  finally { loading.value = false }
}
const checkProfile = async () => {
  checkingProfile.value = true
  profileError.value = ''
  profile.value = null
  try { profile.value = await auth.fetchUser() }
  catch (error) { profileError.value = error instanceof Error ? error.message : String(error) }
  finally { checkingProfile.value = false }
}
onMounted(checkHealth)
</script>

<template>
  <div class="animate-fade-up space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-primary/15 bg-primary-soft/60 px-5 py-4"><div class="flex items-start gap-3"><AppIcon name="play" class="mt-0.5 text-primary" /><p class="text-xs leading-6 text-primary">这是一个真实接口演示。你当前的登录会话，就是认证链路的一部分。</p></div><RouterLink to="/scenario/01-auth/doc" class="flex items-center gap-2 text-xs font-medium text-primary hover:underline">阅读原理<AppIcon name="arrow" class="h-3 w-3" /></RouterLink></div>
    <div class="grid gap-4 lg:grid-cols-3">
      <div v-for="(step, i) in [['建立身份', '登录 / 注册', '校验账号，签发访问与刷新令牌。'], ['访问资源', 'Bearer Token', '请求自动携带凭证，访问受保护接口。'], ['保持会话', 'Silent Refresh', '访问令牌过期时，静默刷新并重试。']]" :key="step[0]" class="rounded-xl border border-line bg-paper p-5"><p class="flex justify-between font-mono text-[10px] text-ink-secondary"><span>STEP 0{{ i + 1 }}</span><AppIcon :name="['lock', 'code', 'refresh'][i]" class="text-primary" /></p><h2 class="mt-4 text-sm font-semibold">{{ step[0] }}</h2><p class="mt-1 font-mono text-[10px] text-primary">{{ step[1] }}</p><p class="mt-3 text-xs leading-6 text-ink-secondary">{{ step[2] }}</p></div>
    </div>
    <div class="grid items-start gap-5 lg:grid-cols-[1.3fr_1fr]">
      <section class="rounded-xl border border-line bg-paper p-6">
        <p class="mb-3 font-mono text-[10px] tracking-widest text-primary">01 / IDENTITY</p><h2 class="text-base font-semibold">验证当前身份</h2><p class="mt-2 text-xs leading-6 text-ink-secondary">向受保护接口发送请求，查看服务端识别出的用户。令牌由统一请求层处理，不在页面中暴露。</p>
        <div class="my-5 flex items-center gap-3 rounded-lg bg-code-bg px-4 py-3 font-mono text-xs"><span class="font-semibold text-primary">GET</span><code>/api/user/me</code></div>
        <button class="flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-xs font-medium text-white hover:bg-primary-hover disabled:opacity-60" :disabled="checkingProfile" @click="checkProfile"><AppIcon name="play" class="h-3 w-3" />{{ checkingProfile ? '请求中…' : '发送身份验证请求' }}</button>
        <div class="mt-5" aria-live="polite"><div v-if="profile" class="rounded-lg border border-success/20 bg-success/5 p-4"><p class="mb-3 flex items-center gap-2 text-xs font-medium text-success"><AppIcon name="check" />身份验证通过</p><dl class="grid grid-cols-[70px_1fr] gap-2 text-xs"><dt class="text-ink-secondary">用户 ID</dt><dd class="break-all font-mono">{{ profile.id }}</dd><dt class="text-ink-secondary">用户名</dt><dd class="break-all">{{ profile.username }}</dd><dt class="text-ink-secondary">昵称</dt><dd class="break-all">{{ profile.nickname }}</dd></dl></div><p v-else-if="profileError" role="alert" class="rounded-lg bg-danger/5 p-3 text-xs text-danger">{{ profileError }}</p><p v-else class="rounded-lg border border-dashed border-line p-5 text-center text-xs text-ink-secondary">等待发送请求 · 响应将在这里展示</p></div>
      </section>
      <div class="space-y-5">
        <section class="rounded-xl border border-line bg-paper p-5"><p class="mb-3 font-mono text-[10px] tracking-widest text-primary">02 / CONNECTION</p><h2 class="text-sm font-semibold">后端健康检查</h2><div class="my-4 flex items-center gap-2 text-xs" aria-live="polite"><span class="h-2 w-2 rounded-full" :class="health?.status === 'UP' ? 'bg-success' : healthError ? 'bg-danger' : 'bg-line'" /><span>{{ loading ? '正在连接服务…' : health?.status === 'UP' ? '服务运行正常' : healthError ? '暂时无法连接' : '等待检查' }}</span></div><p v-if="healthError" role="alert" class="mb-3 break-words text-xs text-danger">{{ healthError }}</p><p v-if="health" class="mb-3 break-all font-mono text-[10px] text-ink-secondary">{{ health.time }}</p><button class="flex items-center gap-2 rounded-lg border border-line px-3 py-2 text-xs hover:bg-code-bg disabled:opacity-60" :disabled="loading" @click="checkHealth"><AppIcon name="refresh" class="h-3 w-3" />{{ loading ? '检查中…' : '重新检查 /api/health' }}</button></section>
        <section class="rounded-xl border border-line bg-paper p-5"><p class="mb-3 font-mono text-[10px] tracking-widest text-primary">03 / LOCAL STATE</p><h2 class="text-sm font-semibold">响应式状态实验</h2><p class="mt-2 text-xs leading-6 text-ink-secondary">体验 Pinia 状态更新。该计数仅存于当前页面会话，不影响服务端数据。</p><div class="mt-4 flex items-center justify-between"><span class="font-mono text-2xl" aria-live="polite">{{ counter.count }}<span class="ml-2 text-[10px] text-ink-secondary">COUNT</span></span><button class="rounded-lg bg-primary-soft px-3 py-2 text-xs font-medium text-primary hover:bg-primary/15" @click="counter.increment()">计数 +1</button></div></section>
      </div>
    </div>
    <p class="text-[11px] leading-6 text-ink-secondary">当前账户：{{ auth.user?.nickname || auth.user?.username }}。若需要切换身份，可通过右上角账户菜单退出登录。</p>
  </div>
</template>
