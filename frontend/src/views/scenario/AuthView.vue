<script setup>
import { onMounted, ref } from 'vue'

import { getHealth } from '@/api/health'
import { useCounterStore } from '@/stores/counter'

const counter = useCounterStore()

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

onMounted(checkHealth)
</script>

<template>
  <section class="mb-4 animate-fade-up rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-2 text-[15px] font-semibold">场景 01 · 用户登录与认证</h2>
    <p class="text-[13px] leading-6 text-ink-secondary">
      JWT 双 token 无感续期：注册 → 登录 → 携带 Bearer 访问受保护接口 → access 过期静默刷新 → 登出。
      设计与取舍见 scenarios/01-auth/NOTES.md。
    </p>
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
