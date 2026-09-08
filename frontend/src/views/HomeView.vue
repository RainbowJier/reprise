<script setup>
import { computed, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { scenarios } from '@/config/scenarios'

const filter = ref('all')
const query = ref('')
const availableCount = scenarios.filter(s => s.enabled).length
const filters = [
  { id: 'all', label: '全部场景', count: scenarios.length },
  { id: 'ready', label: '可体验', count: availableCount },
  { id: 'planned', label: '规划中', count: scenarios.length - availableCount },
]
const visible = computed(() => scenarios.filter(s => {
  const matchesFilter = filter.value === 'all' || (filter.value === 'ready' ? s.enabled : !s.enabled)
  return matchesFilter && `${s.no} ${s.name} ${s.desc} ${(s.tags || []).join(' ')}`.toLowerCase().includes(query.value.trim().toLowerCase())
}).sort((a, b) => Number(b.enabled) - Number(a.enabled)))
const clearFilters = () => { query.value = ''; filter.value = 'all' }
</script>

<template>
  <section class="animate-fade-up">
    <div class="grid items-center gap-8 border-b border-line pb-10 pt-3 xl:grid-cols-[1fr_240px]">
      <div>
        <p class="mb-5 flex items-center gap-2 text-[10px] font-medium tracking-[0.2em] text-primary"><span class="h-1.5 w-1.5 rounded-full bg-primary" />THE ENGINEERING FIELD NOTES</p>
        <h1 class="text-3xl font-semibold leading-[1.4] tracking-tight sm:text-5xl">经典场景，<br><span class="font-display font-normal text-primary">重新理解，亲手复现。</span></h1>
        <p class="mt-5 max-w-xl text-sm leading-7 text-ink-secondary">不止于读懂原理。把登录认证、高并发交易等经典问题，<br class="hidden sm:block">写成可阅读的技术文档，也做成可以操作的真实演示。</p>
        <div class="mt-7 flex flex-wrap items-center gap-5">
          <RouterLink to="/scenario/01-auth/doc" class="inline-flex items-center gap-3 rounded-lg bg-primary px-4 py-2.5 text-xs font-medium text-white hover:bg-primary-hover">从第一个场景开始<AppIcon name="arrow" /></RouterLink>
          <a href="#scenarios" class="inline-flex items-center gap-2 text-xs text-ink-secondary hover:text-primary">探索场景集合<AppIcon name="chevron" class="h-3 w-3" /></a>
        </div>
      </div>
      <div class="hidden rounded-xl border border-line bg-paper p-5 xl:block">
        <div class="mb-5 flex items-center justify-between"><span class="font-mono text-[10px] text-ink-secondary">THE REPRISE METHOD</span><AppIcon name="code" class="text-primary" /></div>
        <div v-for="(step, i) in [['理解问题', '从真实需求出发'], ['拆解设计', '记录方案与取舍'], ['运行验证', '让原理变成体验']]" :key="step[0]" class="flex gap-3 py-3"><span class="pt-0.5 font-mono text-[10px] text-primary/60">0{{ i + 1 }}</span><div><p class="text-sm font-medium">{{ step[0] }}</p><p class="mt-1 text-xs text-ink-secondary">{{ step[1] }}</p></div></div>
        <p class="mt-3 border-t border-line pt-3 font-display text-lg italic text-primary">Read. Build. Repeat.</p>
      </div>
    </div>

    <div id="scenarios" class="scroll-mt-24 pt-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-3"><h2 class="text-lg font-semibold tracking-tight">场景集合</h2><span class="font-mono text-xs text-ink-secondary">{{ String(scenarios.length).padStart(2, '0') }}</span></div>
        <label class="flex w-full items-center gap-2 rounded-lg border border-line bg-paper px-3 py-2 text-ink-secondary sm:w-60 focus-within:border-primary"><AppIcon name="search" /><input v-model="query" type="search" class="min-w-0 flex-1 bg-transparent text-xs outline-none placeholder:text-ink-secondary/70" placeholder="搜索场景、技术关键词…" aria-label="搜索场景" /></label>
      </div>
      <div class="my-5 flex flex-wrap items-center gap-1" aria-label="场景状态筛选">
        <button v-for="item in filters" :key="item.id" class="flex items-center gap-2 rounded-md px-3 py-2 text-xs" :aria-pressed="filter === item.id" :class="filter === item.id ? 'bg-primary-soft font-medium text-primary' : 'text-ink-secondary hover:bg-paper'" @click="filter = item.id">{{ item.label }}<span class="font-mono text-[10px] opacity-70">{{ item.count }}</span></button>
        <span class="ml-auto hidden text-[11px] text-ink-secondary sm:block">文档与演示，成对收录</span>
      </div>
      <p class="sr-only" aria-live="polite">找到 {{ visible.length }} 个场景</p>
      <div v-if="visible.length" class="grid gap-4 sm:grid-cols-2">
        <article v-for="s in visible" :key="s.id" class="group flex flex-col rounded-xl border border-line p-5 transition-colors sm:p-6" :class="s.enabled ? 'bg-paper hover:border-primary/40' : 'bg-canvas'">
          <div class="flex items-center justify-between"><span class="flex h-10 w-10 items-center justify-center rounded-xl" :class="s.enabled ? 'bg-primary-soft text-primary' : 'border border-line text-ink-secondary'"><AppIcon :name="s.icon" class="h-5 w-5" /></span><span class="flex items-center gap-1.5 rounded-full px-2 py-1 text-[10px]" :class="s.enabled ? 'bg-primary-soft text-primary' : 'text-ink-secondary'"><span class="h-1 w-1 rounded-full" :class="s.enabled ? 'bg-success' : 'bg-ink-secondary/40'" />{{ s.enabled ? '已收录 · 可体验' : '规划中' }}</span></div>
          <p class="mb-2 mt-5 font-mono text-[10px] tracking-widest text-ink-secondary">{{ s.no }} / {{ s.category }}</p>
          <h3 class="text-lg font-semibold"><RouterLink v-if="s.enabled" :to="`${s.path}/doc`" class="hover:text-primary">{{ s.name }}</RouterLink><template v-else>{{ s.name }}</template></h3>
          <p class="mb-4 mt-2 text-xs leading-6 text-ink-secondary">{{ s.desc }}</p>
          <div class="mb-6 mt-auto flex flex-wrap gap-1.5"><span v-for="tag in s.tags" :key="tag" class="rounded border border-line px-2 py-0.5 text-[10px] text-ink-secondary">{{ tag }}</span></div>
          <div v-if="s.enabled" class="flex flex-wrap items-center gap-5 border-t border-line pt-4 text-xs"><RouterLink :to="`${s.path}/doc`" class="flex items-center gap-2 font-medium text-primary hover:underline"><AppIcon name="book" class="h-3.5 w-3.5" />阅读文档</RouterLink><RouterLink :to="s.path" class="flex items-center gap-2 text-ink-secondary hover:text-primary"><AppIcon name="play" class="h-3.5 w-3.5" />交互演示</RouterLink><AppIcon name="arrow" class="ml-auto text-primary/40 transition-transform group-hover:translate-x-1" /></div>
          <p v-else class="border-t border-line pt-4 text-[11px] text-ink-secondary">尚未开放 · 实现与文档将同步收录</p>
        </article>
      </div>
      <div v-else class="rounded-xl border border-dashed border-line bg-paper px-6 py-16 text-center"><AppIcon name="search" class="mx-auto mb-4 h-7 w-7 text-ink-secondary" /><h3 class="font-medium">没有找到匹配的场景</h3><p class="mt-2 text-sm text-ink-secondary">试试“JWT”“库存”，或切换场景状态。</p><button class="mt-5 rounded-lg bg-primary-soft px-4 py-2 text-sm text-primary" @click="clearFilters">清除筛选条件</button></div>
    </div>
  </section>
</template>
