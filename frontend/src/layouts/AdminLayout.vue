<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { scenarios } from '@/config/scenarios'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileOpen = ref(false)
const menuButton = ref(null)
const mobileClose = ref(null)
const userMenuOpen = ref(false)
const expandedId = ref(null)
const currentScenario = computed(() => scenarios.find(s => s.enabled && (route.path === s.path || route.path === `${s.path}/doc`)))
const available = scenarios.filter(s => s.enabled)
const planned = scenarios.filter(s => !s.enabled)
const isDoc = computed(() => route.path.endsWith('/doc'))
const avatarChar = computed(() => (auth.user?.nickname || auth.user?.username || '?').slice(0, 1))
const childrenOf = s => [
  { label: '技术说明文档', path: `${s.path}/doc`, icon: 'book' },
  { label: '详情页效果展示', path: s.path, icon: 'play' },
]

watch(() => route.path, () => {
  mobileOpen.value = false
  userMenuOpen.value = false
  expandedId.value = currentScenario.value?.id || null
}, { immediate: true })
watch(mobileOpen, async open => {
  document.body.style.overflow = open ? 'hidden' : ''
  if (open) { await nextTick(); mobileClose.value?.focus() }
})
onBeforeUnmount(() => { document.body.style.overflow = '' })
const closeNavigation = () => { mobileOpen.value = false; menuButton.value?.focus() }
const trapNavigation = event => {
  if (!mobileOpen.value || event.key !== 'Tab') return
  const controls = [...event.currentTarget.querySelectorAll('button:not(:disabled),a[href]')].filter(el => el.getClientRects().length)
  const first = controls[0]
  const last = controls[controls.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
const logout = () => { auth.logout(); router.push('/login') }
</script>

<template>
  <div class="min-h-dvh bg-canvas" @keydown.esc="userMenuOpen = false">
    <a href="#main-content" class="sr-only z-50 rounded-lg bg-primary p-3 text-white focus:not-sr-only focus:fixed focus:left-4 focus:top-4">跳至内容</a>
    <header class="sticky top-0 z-30 border-b border-line bg-canvas/95 backdrop-blur-xl">
      <div class="mx-auto flex h-18 max-w-[1600px] items-center gap-4 px-5 lg:px-8">
        <button ref="menuButton" class="rounded-lg p-2 text-ink-secondary lg:hidden" aria-label="打开场景导航" :aria-expanded="mobileOpen" aria-controls="library-navigation" @click="mobileOpen = true"><AppIcon name="menu" class="h-5 w-5" /></button>
        <RouterLink to="/" class="flex items-center gap-3" aria-label="reprise 场景集合首页">
          <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-primary font-mono text-lg font-bold text-white">r.</span>
          <span class="text-xl font-semibold tracking-tight">reprise<span class="ml-2 hidden rounded border border-line px-1.5 py-0.5 align-middle font-mono text-[10px] font-normal text-ink-secondary sm:inline">field notes</span></span>
        </RouterLink>
        <span class="ml-8 hidden text-xs text-ink-secondary xl:block">复现经典场景 · 记录工程思考</span>
        <div class="ml-auto flex items-center gap-3">
          <RouterLink to="/" class="hidden items-center gap-2 rounded-lg px-3 py-2 text-xs text-ink-secondary hover:bg-primary-soft hover:text-primary sm:flex"><AppIcon name="grid" />场景集合</RouterLink>
          <span class="hidden h-5 w-px bg-line sm:block" />
          <div v-if="auth.user" class="relative">
            <button class="relative z-40 flex items-center gap-2 rounded-full py-1 pl-1 pr-2 text-sm hover:bg-white" :aria-expanded="userMenuOpen" aria-controls="account-menu" aria-label="账户菜单" @click="userMenuOpen = !userMenuOpen">
              <span class="flex h-8 w-8 items-center justify-center rounded-full border border-primary/10 bg-primary-soft text-xs font-semibold text-primary">{{ avatarChar }}</span>
              <span class="hidden max-w-28 truncate text-xs sm:block">{{ auth.user.nickname || auth.user.username }}</span><AppIcon name="chevron" class="h-3 w-3 text-ink-secondary" />
            </button>
            <div v-if="userMenuOpen" class="fixed inset-0 z-30" @click="userMenuOpen = false" />
            <Transition name="pop"><div v-if="userMenuOpen" id="account-menu" class="absolute right-0 top-12 z-40 w-48 rounded-xl border border-line bg-paper p-2 shadow-lg">
              <p class="truncate px-3 py-2 text-xs text-ink-secondary">@{{ auth.user.username }}</p>
              <button class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-danger hover:bg-danger/5" @click="logout"><AppIcon name="logout" />退出登录</button>
            </div></Transition>
          </div>
        </div>
      </div>
    </header>

    <div class="mx-auto flex max-w-[1600px] items-start">
      <div v-if="mobileOpen" class="fixed inset-0 z-40 bg-ink/30 backdrop-blur-sm lg:hidden" @click="closeNavigation" />
      <aside id="library-navigation" class="fixed inset-y-0 left-0 z-50 flex w-68 shrink-0 flex-col border-r border-line bg-canvas p-5 lg:sticky lg:top-18 lg:z-10 lg:h-[calc(100dvh-4.5rem)] lg:w-64 lg:translate-x-0 lg:px-6 lg:py-8" :class="mobileOpen ? 'flex' : 'hidden lg:flex'" :role="mobileOpen ? 'dialog' : undefined" :aria-modal="mobileOpen || undefined" aria-label="场景导航" @keydown="trapNavigation" @keydown.esc.stop="closeNavigation">
        <div class="mb-6 flex items-center justify-between lg:hidden"><span class="font-semibold">场景导航</span><button ref="mobileClose" class="rounded p-2" aria-label="关闭场景导航" @click="closeNavigation"><AppIcon name="close" /></button></div>
        <RouterLink to="/" class="mb-7 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium" :class="!currentScenario ? 'bg-primary-soft text-primary' : 'text-ink-secondary hover:bg-primary-soft'"><AppIcon name="grid" />场景总览<span class="ml-auto font-mono text-[10px]">{{ scenarios.length }}</span></RouterLink>
        <nav class="min-h-0 flex-1 overflow-y-auto" aria-label="场景目录">
          <p class="mb-3 px-3 text-[10px] font-semibold tracking-[0.18em] text-ink-secondary">已收录 / AVAILABLE</p>
          <div v-for="s in available" :key="s.id" class="mb-2">
            <button class="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-[13px] hover:bg-primary-soft" :aria-expanded="expandedId === s.id" :aria-controls="`nav-${s.id}`" @click="expandedId = expandedId === s.id ? null : s.id">
              <span class="font-mono text-[10px] text-ink-secondary">{{ s.no }}</span><span class="flex-1 font-medium">{{ s.name }}</span><AppIcon name="chevron" class="h-3 w-3 text-ink-secondary transition-transform" :class="expandedId === s.id ? 'rotate-180' : ''" />
            </button>
            <div v-if="expandedId === s.id" :id="`nav-${s.id}`" class="ml-6 border-l border-line pl-3">
              <RouterLink v-for="c in childrenOf(s)" :key="c.path" :to="c.path" class="my-1 flex items-center gap-2 rounded-md px-2.5 py-2 text-xs" :class="route.path === c.path ? 'bg-primary-soft font-medium text-primary' : 'text-ink-secondary hover:text-primary'"><AppIcon :name="c.icon" class="h-3.5 w-3.5" />{{ c.label }}</RouterLink>
            </div>
          </div>
          <p class="mb-3 mt-7 px-3 text-[10px] font-semibold tracking-[0.18em] text-ink-secondary">探索路线 / NEXT UP</p>
          <div v-for="s in planned" :key="s.id" class="flex items-center gap-2.5 px-3 py-2 text-xs text-ink-secondary"><span class="font-mono text-[10px]">{{ s.no }}</span><span>{{ s.name }}</span><span class="ml-auto h-1 w-1 rounded-full bg-ink-secondary/30" /></div>
        </nav>
        <div class="mt-5 border-t border-line px-3 pt-5"><p class="flex items-center gap-2 text-[11px] text-ink-secondary"><span class="h-1.5 w-1.5 rounded-full bg-success" />持续构建中<span class="ml-auto font-mono">{{ available.length }} / {{ scenarios.length }}</span></p><div class="mt-3 h-1 rounded-full bg-line"><div class="h-full rounded-full bg-primary" :style="{ width: `${available.length / scenarios.length * 100}%` }" /></div></div>
      </aside>

      <main id="main-content" tabindex="-1" class="min-w-0 flex-1 px-5 py-8 outline-none sm:px-8 lg:px-10 lg:py-10 xl:px-14">
        <div class="mx-auto max-w-[1120px]">
          <header v-if="currentScenario" class="mb-8">
            <nav aria-label="面包屑" class="mb-6 flex flex-wrap items-center gap-2 text-xs text-ink-secondary"><RouterLink to="/" class="hover:text-primary">场景集合</RouterLink><span>/</span><span>{{ currentScenario.name }}</span></nav>
            <div class="mb-4 flex items-center gap-2 text-[10px] font-medium tracking-widest text-primary"><span class="font-mono">SCENARIO {{ currentScenario.no }}</span><span class="h-px w-6 bg-primary/30" /><span>{{ currentScenario.category }}</span></div>
            <h1 class="text-3xl font-semibold tracking-tight sm:text-4xl">{{ currentScenario.name }}</h1>
            <p class="mt-4 max-w-2xl text-sm leading-7 text-ink-secondary">{{ currentScenario.desc }}</p>
            <div class="mt-7 flex items-center gap-6 border-b border-line">
              <RouterLink :to="`${currentScenario.path}/doc`" class="-mb-px flex items-center gap-2 border-b-2 py-3 text-sm" :class="isDoc ? 'border-primary font-semibold text-primary' : 'border-transparent text-ink-secondary hover:text-ink'"><AppIcon name="book" />技术文档</RouterLink>
              <RouterLink :to="currentScenario.path" class="-mb-px flex items-center gap-2 border-b-2 py-3 text-sm" :class="!isDoc ? 'border-primary font-semibold text-primary' : 'border-transparent text-ink-secondary hover:text-ink'"><AppIcon name="play" />交互演示</RouterLink>
              <span class="ml-auto hidden font-mono text-[10px] text-ink-secondary sm:block">READ. BUILD. REPEAT.</span>
            </div>
          </header>
          <RouterView />
          <footer class="mt-16 flex flex-wrap items-center justify-between gap-3 border-t border-line py-6 text-[11px] text-ink-secondary"><span>reprise · 每个场景，都是一次重新理解。</span><span class="font-mono">Vue + Spring Boot</span></footer>
        </div>
      </main>
    </div>
  </div>
</template>
