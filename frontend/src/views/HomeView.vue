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
  <section class="card">
    <h2>后端健康检查</h2>
    <div class="row">
      <button :disabled="loading" @click="checkHealth">
        {{ loading ? '检查中…' : '检查 /api/health' }}
      </button>
      <template v-if="health">
        <span class="dot" :class="health.status === 'UP' ? 'up' : 'down'" />
        <span>{{ health.status }}</span>
        <span class="muted">{{ health.time }}</span>
      </template>
      <span v-else-if="healthError" class="error">{{ healthError }}</span>
      <span v-else class="muted">尚未检查</span>
    </div>
  </section>

  <section class="card">
    <h2>状态管理（Pinia）</h2>
    <div class="row">
      <button @click="counter.increment()">count + 1</button>
      <span>count = <code>{{ counter.count }}</code></span>
    </div>
  </section>
</template>
