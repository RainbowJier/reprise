<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { scenarios } from '@/config/scenarios'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const userMenuOpen = ref(false)

// 每个已实现场景的两个子菜单（约定式路径）
const childrenOf = (s) => [
  { label: '详情页效果展示', path: s.path },
  { label: '技术说明文档', path: `${s.path}/doc` },
]

const currentScenario = computed(() =>
  scenarios.find((s) => s.enabled && (route.path === s.path || route.path === `${s.path}/doc`)),
)

const currentChildLabel = computed(() => {
  if (!currentScenario.value) {
    return ''
  }
  return childrenOf(currentScenario.value).find((c) => c.path === route.path)?.label ?? ''
})

// 手风琴：默认展开当前路由所属场景
const expandedId = ref(currentScenario.value?.id ?? null)
watch(
  () => currentScenario.value?.id,
  (id) => {
    if (id) {
      expandedId.value = id
    }
  },
  { immediate: true },
)

// 路由变化时收起用户下拉，避免残留遮罩
watch(
  () => route.path,
  () => {
    userMenuOpen.value = false
  },
)

const toggleExpand = (s) => {
  if (!s.enabled) {
    return
  }
  expandedId.value = expandedId.value === s.id ? null : s.id
}

// 头像取昵称首字符（中文昵称即单字）
const avatarChar = computed(() => (auth.user?.nickname || '?').slice(0, 1))

const logout = () => {
  userMenuOpen.value = false
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <!-- 左侧：品牌 + 场景菜单（高度固定为视口，菜单区内部滚动） -->
    <aside class="flex w-56 shrink-0 flex-col bg-sidebar">
      <div class="flex h-14 shrink-0 items-center border-b border-white/10 px-5">
        <span class="text-base font-bold text-white">reprise</span>
        <span class="ml-2 text-xs text-sidebar-text/70">场景复现</span>
      </div>
      <nav class="flex-1 overflow-y-auto py-2">
        <div v-for="s in scenarios" :key="s.id">
          <!-- 父菜单：点击展开/收起 -->
          <button
            class="flex w-full items-center gap-2.5 px-4 py-2.5 text-sm transition-colors duration-150"
            :class="s.enabled
              ? 'text-sidebar-text hover:bg-sidebar-hover hover:text-white'
              : 'cursor-not-allowed text-sidebar-text/40'"
            :title="s.enabled ? s.name : `${s.name}（规划中）`"
            @click="toggleExpand(s)"
          >
            <span class="rounded bg-white/10 px-1.5 py-0.5 font-mono text-[10px]">{{ s.no }}</span>
            <span>{{ s.name }}</span>
            <span v-if="!s.enabled" class="ml-auto rounded-full border border-sidebar-text/20 px-2 py-0.5 text-[10px]">规划中</span>
            <span
              v-else
              class="ml-auto text-[10px] transition-transform duration-200"
              :class="[expandedId === s.id ? 'rotate-180' : '', currentScenario?.id === s.id ? 'text-white' : 'text-sidebar-text/60']"
            >▾</span>
          </button>

          <!-- 子菜单：grid 高度动画平滑展开/收起 -->
          <div
            v-if="s.enabled"
            class="grid transition-[grid-template-rows] duration-200 ease-in-out"
            :class="expandedId === s.id ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
          >
            <div
              class="overflow-hidden transition-opacity duration-200"
              :class="expandedId === s.id ? 'opacity-100' : 'opacity-0'"
            >
              <RouterLink
                v-for="c in childrenOf(s)"
                :key="c.path"
                :to="c.path"
                class="flex items-center border-l-4 border-transparent py-2 pl-11 pr-4 text-[13px] transition-colors duration-150"
                :class="route.path === c.path
                  ? 'border-primary bg-sidebar-active text-white'
                  : 'text-sidebar-text hover:bg-sidebar-hover hover:text-white'"
              >
                {{ c.label }}
              </RouterLink>
            </div>
          </div>
        </div>
      </nav>
    </aside>

    <!-- 右侧：顶栏 + 内容区 -->
    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex h-14 shrink-0 items-center justify-between border-b border-line bg-white px-6">
        <div class="flex items-center gap-2 text-sm">
          <span class="text-ink-secondary">场景复现</span>
          <template v-if="currentScenario">
            <span class="text-ink-secondary">/</span>
            <span>{{ currentScenario.no }} {{ currentScenario.name }}</span>
            <template v-if="currentChildLabel">
              <span class="text-ink-secondary">/</span>
              <span class="text-ink-secondary">{{ currentChildLabel }}</span>
            </template>
          </template>
        </div>

        <div v-if="auth.user" class="relative">
          <button
            class="flex items-center gap-2 rounded-md px-3 py-1.5 text-sm hover:bg-canvas"
            @click="userMenuOpen = !userMenuOpen"
          >
            <span class="inline-block h-6 w-6 rounded-full bg-primary text-center text-xs leading-6 text-white">{{ avatarChar }}</span>
            <span>{{ auth.user.nickname }}</span>
            <span
              class="text-[10px] text-ink-secondary transition-transform duration-200"
              :class="userMenuOpen ? 'rotate-180' : ''"
            >▾</span>
          </button>
          <!-- 点击空白处关闭下拉 -->
          <Transition name="pop" :duration="180">
            <div v-if="userMenuOpen" class="fixed inset-0 z-10" @click="userMenuOpen = false" />
          </Transition>
          <Transition name="pop" :duration="180">
            <div
              v-if="userMenuOpen"
              class="absolute right-0 top-full z-20 mt-1 w-36 origin-top-right rounded-md border border-line bg-white py-1 shadow-sm"
            >
              <div class="px-3 py-1.5 text-xs text-ink-secondary">@{{ auth.user.username }}</div>
              <button
                class="block w-full px-3 py-1.5 text-left text-sm text-danger hover:bg-canvas"
                @click="logout"
              >
                退出登录
              </button>
            </div>
          </Transition>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto p-6">
        <!-- 视图入场动画由各页面根元素 animate-fade-up 自带：无 Transition 状态机，
             懒加载视图切换时不会因过渡被打断而卡在空内容 -->
        <RouterView />
      </main>
    </div>
  </div>
</template>
