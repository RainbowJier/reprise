<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { getAccessToken } from '@/api/authTokens'
import { listFlashItems, listMyFlashOrders, resetFlashDemoItem, seckillFlashItem } from '@/api/flashSale'
import { raceSeckill, registerRaceUser } from '@/api/flashRace'

const items = ref([])
const orders = ref([])
const listError = ref('')
const loading = ref(false)
const busyItemId = ref(null)
const result = ref(null)
const autoRefresh = ref(true)
const now = ref(Date.now())

let tickTimer = null
let refreshTimer = null

const STATUS_META = {
  NOT_STARTED: { label: '未开始', badge: 'bg-code-bg text-ink-secondary' },
  IN_PROGRESS: { label: '进行中', badge: 'bg-primary/10 text-primary' },
  ENDED: { label: '已结束', badge: 'bg-code-bg text-ink-secondary' },
}

const soldOut = (item) => item.status === 'IN_PROGRESS' && item.stock === 0
const soldPct = (item) =>
  item.totalStock > 0 ? Math.round(((item.totalStock - item.stock) / item.totalStock) * 100) : 100

const fmtPrice = (cents) => `¥${(cents / 100).toFixed(2)}`
const fmtTime = (ts) => new Date(ts).toLocaleString('zh-CN', { hour12: false })

// 倒计时：未开始显示距开抢，进行中显示距结束（展示层体验优化，按钮可用性以后端状态为准）
const countdown = (item) => {
  const target =
    item.status === 'NOT_STARTED' ? new Date(item.startTime).getTime() : new Date(item.endTime).getTime()
  const diff = target - now.value
  if (diff <= 0) return ''
  const sec = Math.floor(diff / 1000)
  const pad = (n) => String(n).padStart(2, '0')
  const days = Math.floor(sec / 86400)
  const hms = `${pad(Math.floor((sec % 86400) / 3600))}:${pad(Math.floor((sec % 3600) / 60))}:${pad(sec % 60)}`
  return days > 0 ? `${days}天 ${hms}` : hms
}

const buttonState = (item) => {
  if (busyItemId.value === item.id) return { label: '抢购中…', disabled: true }
  if (item.mine) return { label: '已抢购', disabled: true, cls: 'bg-success' }
  if (item.status === 'NOT_STARTED') return { label: '未开始', disabled: true }
  if (item.status === 'ENDED') return { label: '已结束', disabled: true }
  if (soldOut(item)) return { label: '已售罄', disabled: true }
  return { label: '立即抢购', disabled: false, cls: 'bg-primary hover:bg-primary-hover' }
}

async function loadItems(silent = false) {
  if (!silent) loading.value = true
  if (!silent) listError.value = ''
  try {
    items.value = await listFlashItems()
  } catch (error) {
    if (!silent) listError.value = error instanceof Error ? error.message : String(error)
  } finally {
    loading.value = false
  }
}

async function loadOrders() {
  try {
    orders.value = await listMyFlashOrders()
  } catch {
    orders.value = []
  }
}

async function handleSeckill(item) {
  busyItemId.value = item.id
  result.value = null
  try {
    const order = await seckillFlashItem(item.id)
    result.value = { type: 'success', text: `抢购成功：${order.itemName}（订单号 ${order.orderId}）` }
    await Promise.all([loadItems(true), loadOrders()])
  } catch (error) {
    result.value = { type: 'error', text: error instanceof Error ? error.message : String(error) }
    // 失败多为售罄/重复抢购，静默刷新最新库存与 mine 标记
    loadItems(true)
  } finally {
    busyItemId.value = null
  }
}

function applyAutoRefresh() {
  if (refreshTimer) clearInterval(refreshTimer)
  if (autoRefresh.value) refreshTimer = setInterval(() => loadItems(true), 2000)
}

onMounted(() => {
  loadItems()
  loadOrders()
  tickTimer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
  applyAutoRefresh()
})

watch(autoRefresh, applyAutoRefresh)

onBeforeUnmount(() => {
  clearInterval(tickTimer)
  clearInterval(refreshTimer)
})

// ===== 并发抢购演示：浏览器端模拟洪峰 =====

const RACE_MODES = [
  { key: 'storm', label: '多用户洪峰' },
  { key: 'burst', label: '单用户连点' },
]
const BURST_TIMES = 12
const REGISTER_BATCH = 8

const raceMode = ref('storm')
const raceUsers = ref(24)
const raceItemId = ref(null)
const racePhase = ref('idle') // idle | registering | firing | done
const raceProgress = ref('')
const raceError = ref('')
const raceStats = ref(null) // { total, wins, soldOut, duplicate, other, winUsers, elapsed, avgRt }
const raceLogs = ref([])
const raceInitialStock = ref(0)
const raceServerStock = ref(null)

const raceBusy = computed(() => racePhase.value === 'registering' || racePhase.value === 'firing')
const racableItems = computed(() => items.value.filter((i) => i.status === 'IN_PROGRESS'))
const raceTarget = computed(() => items.value.find((i) => i.id === raceItemId.value))
const canStartRace = computed(() => !raceBusy.value && !!raceTarget.value && raceTarget.value.stock > 0)

// 不变式对账：初始库存 − 成功数 应等于服务端剩余（null = 尚未取到服务端数据）
const invariantOk = computed(() => {
  if (!raceStats.value || raceServerStock.value === null) return null
  return raceInitialStock.value - raceStats.value.wins === raceServerStock.value
})

const logClass = (code) => {
  if (code === 200) return 'text-success'
  if (code === 6103) return 'text-danger'
  if (code === 6104) return 'text-primary'
  return 'text-ink-secondary'
}

// 目标商品默认耳机（id=2，库存小、售罄快，最适合演示）
watch(items, (list) => {
  if (!list.length) return
  const ok = list.some((i) => i.id === raceItemId.value && i.status === 'IN_PROGRESS')
  if (!ok) {
    const preferred = list.find((i) => i.id === 2 && i.status === 'IN_PROGRESS')
    raceItemId.value = (preferred ?? list.find((i) => i.status === 'IN_PROGRESS'))?.id ?? null
  }
})

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const pct = (n) => `${Math.round((n / Math.max(1, raceStats.value?.total ?? 1)) * 100)}%`

async function startRace() {
  const target = raceTarget.value
  if (!target || target.stock <= 0) return

  raceError.value = ''
  raceLogs.value = []
  raceServerStock.value = null
  raceStats.value = null
  raceInitialStock.value = target.stock

  const stats = { total: 0, wins: 0, soldOut: 0, duplicate: 0, other: 0, winUsers: [], elapsed: 0, avgRt: 0 }
  const rts = []
  const t0 = performance.now()
  const record = (r, username) => {
    stats.total += 1
    if (r.code === 200) {
      stats.wins += 1
      stats.winUsers.push(username)
    } else if (r.code === 6103) {
      stats.soldOut += 1
    } else if (r.code === 6104) {
      stats.duplicate += 1
    } else {
      stats.other += 1
    }
    rts.push(r.rt)
    if (raceLogs.value.length < 100) {
      raceLogs.value.push({ t: Math.round(performance.now() - t0), user: username, ...r })
    }
  }

  try {
    if (raceMode.value === 'storm') {
      // 多用户洪峰：批量注册虚拟用户（分批 8 并发，BCrypt 注册有成本），再同时开抢
      const n = Math.min(50, Math.max(4, Number(raceUsers.value) || 24))
      racePhase.value = 'registering'
      raceProgress.value = `注册并发用户 0/${n}`
      const stamp = Date.now()
      const users = []
      for (let i = 0; i < n; i += REGISTER_BATCH) {
        const size = Math.min(REGISTER_BATCH, n - i)
        const batch = await Promise.all(
          Array.from({ length: size }, (_, k) => registerRaceUser(`race${stamp}_${i + k}`)),
        )
        users.push(...batch)
        raceProgress.value = `注册并发用户 ${users.length}/${n}`
      }

      racePhase.value = 'firing'
      raceProgress.value = `${users.length} 个用户同时开抢（随机 0~400ms 起跑差）`
      await Promise.all(
        users.map(async (u) => {
          await sleep(Math.random() * 400)
          record(await raceSeckill(target.id, u.accessToken), u.username)
        }),
      )
    } else {
      // 单用户连点：当前登录用户对同一商品并发提交，演示限购兜底
      const token = getAccessToken()
      if (!token) throw new Error('请先登录')
      racePhase.value = 'firing'
      raceProgress.value = `当前用户 ${BURST_TIMES} 连击并发提交`
      await Promise.all(
        Array.from({ length: BURST_TIMES }, () => raceSeckill(target.id, token).then((r) => record(r, '当前用户'))),
      )
    }
  } catch (error) {
    raceError.value = error instanceof Error ? error.message : String(error)
  }

  racePhase.value = 'done'
  raceProgress.value = ''
  stats.elapsed = Math.round(performance.now() - t0)
  stats.avgRt = rts.length ? Math.round(rts.reduce((a, b) => a + b, 0) / rts.length) : 0
  raceStats.value = { ...stats }

  // 服务端对账：拉最新列表取真实剩余库存（顺带刷新商品卡片与 mine 标记）；
  // 单用户连点模式下当前用户可能抢到，订单区一并刷新
  await Promise.all([loadItems(true), loadOrders()])
  raceServerStock.value = raceTarget.value?.stock ?? null
}

async function resetRaceItem() {
  if (!raceItemId.value || raceBusy.value) return
  raceError.value = ''
  try {
    await resetFlashDemoItem(raceItemId.value)
    raceServerStock.value = null
    raceStats.value = null
    raceLogs.value = []
    result.value = { type: 'success', text: '演示库存已重置（库存回满、订单清空、售罄标记清除）' }
    await loadItems(true)
  } catch (error) {
    raceError.value = error instanceof Error ? error.message : String(error)
  }
}
</script>

<template>
  <section class="mb-4 animate-fade-up rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-2 text-[15px] font-semibold">场景 03 · 秒杀 / 高并发抢购</h2>
    <p class="text-sm leading-6 text-ink-secondary">
      防超卖三层防线：内存售罄标记（拦洪峰）→ 数据库原子扣减（保正确）→ 唯一索引限购（幂等兜底）。
      页面内建并发演示控制台，可实时观察整个抢购过程；金额单位为分；设计见 scenarios/03-flash-sale/design.md。
    </p>
  </section>

  <section class="mb-4 rounded-card border border-line bg-white px-6 py-5">
    <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
      <h2 class="text-[15px] font-semibold">秒杀商品</h2>
      <div class="flex items-center gap-3">
        <label class="flex cursor-pointer items-center gap-1.5 text-sm text-ink-secondary">
          <input v-model="autoRefresh" type="checkbox" class="accent-primary" />
          自动刷新库存
        </label>
        <button
          class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
          @click="loadItems()"
        >
          {{ loading ? '刷新中…' : '刷新' }}
        </button>
      </div>
    </div>

    <p v-if="listError" class="mb-3 text-sm text-danger">{{ listError }}</p>

    <div
      v-if="result"
      class="mb-3 rounded-md px-3 py-2 text-sm"
      :class="result.type === 'success' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'"
    >
      {{ result.text }}
    </div>

    <p v-if="!items.length && !listError" class="text-sm text-ink-secondary">暂无秒杀商品</p>

    <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2">
      <article v-for="item in items" :key="item.id" class="rounded-card border border-line p-4">
        <div class="mb-2 flex items-start justify-between gap-2">
          <div>
            <h3 class="text-[15px] font-semibold">{{ item.name }}</h3>
            <p class="mt-0.5 text-xs text-ink-secondary">
              {{ fmtTime(item.startTime) }} ~ {{ fmtTime(item.endTime) }}
            </p>
          </div>
          <span
            class="shrink-0 rounded-full px-2.5 py-0.5 text-xs"
            :class="STATUS_META[item.status]?.badge ?? 'bg-code-bg text-ink-secondary'"
          >
            {{ STATUS_META[item.status]?.label ?? item.status }}
          </span>
        </div>

        <div class="mb-3 flex items-baseline gap-2">
          <span class="text-lg font-semibold text-danger">{{ fmtPrice(item.price) }}</span>
          <span class="text-sm text-ink-secondary line-through">{{ fmtPrice(item.originalPrice) }}</span>
        </div>

        <div class="mb-1 flex items-center justify-between text-xs text-ink-secondary">
          <span>已抢 {{ item.totalStock - item.stock }} / {{ item.totalStock }} 件</span>
          <span v-if="item.status === 'NOT_STARTED'">{{ countdown(item) }} 后开始</span>
          <span v-else-if="item.status === 'IN_PROGRESS' && !soldOut(item)">{{ countdown(item) }} 后结束</span>
        </div>
        <div class="mb-4 h-1.5 overflow-hidden rounded-full bg-code-bg">
          <div
            class="h-full rounded-full bg-primary transition-all duration-300"
            :style="{ width: `${soldPct(item)}%` }"
          />
        </div>

        <button
          class="w-full rounded-md py-2 text-sm text-white transition-all disabled:cursor-not-allowed disabled:opacity-60"
          :class="buttonState(item).cls ?? 'bg-primary'"
          :disabled="buttonState(item).disabled"
          @click="handleSeckill(item)"
        >
          {{ buttonState(item).label }}
        </button>
      </article>
    </div>
  </section>

  <section class="mb-4 rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-1 text-[15px] font-semibold">并发抢购演示</h2>
    <p class="mb-4 text-sm text-ink-secondary">
      浏览器端真实发起洪峰：批量注册虚拟用户同时开抢，实时展示库存扣减、售罄拒绝与限购兜底，结束后与服务端对账。
    </p>

    <div class="mb-3 flex flex-wrap items-center gap-3">
      <div class="flex rounded-md border border-line p-0.5">
        <button
          v-for="m in RACE_MODES"
          :key="m.key"
          type="button"
          class="rounded px-3 py-1.5 text-sm disabled:cursor-not-allowed"
          :class="raceMode === m.key ? 'bg-primary text-white' : 'text-ink-secondary hover:text-ink'"
          :disabled="raceBusy"
          @click="raceMode = m.key"
        >
          {{ m.label }}
        </button>
      </div>

      <label v-if="raceMode === 'storm'" class="flex items-center gap-1.5 text-sm text-ink-secondary">
        并发用户
        <input
          v-model.number="raceUsers"
          type="number"
          min="4"
          max="50"
          :disabled="raceBusy"
          class="w-16 rounded-md border border-line px-2 py-1 text-sm text-ink"
        />
      </label>

      <label class="flex items-center gap-1.5 text-sm text-ink-secondary">
        目标商品
        <select
          v-model.number="raceItemId"
          :disabled="raceBusy"
          class="rounded-md border border-line bg-white px-2 py-1 text-sm text-ink"
        >
          <option v-for="i in racableItems" :key="i.id" :value="i.id">
            {{ i.name }}（库存 {{ i.stock }}）
          </option>
        </select>
      </label>

      <button
        class="rounded-md bg-primary px-4 py-2 text-sm text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="!canStartRace"
        @click="startRace"
      >
        开始并发抢购
      </button>
      <button
        class="rounded-md border border-line px-4 py-2 text-sm text-ink-secondary hover:bg-code-bg disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="!raceItemId || raceBusy"
        @click="resetRaceItem"
      >
        重置演示库存
      </button>
      <span v-if="raceProgress" class="animate-pulse text-sm text-primary">{{ raceProgress }}</span>
    </div>

    <p v-if="raceError" class="mb-3 text-sm text-danger">{{ raceError }}</p>
    <p v-else-if="racePhase === 'idle' && !raceStats" class="mb-3 text-xs text-ink-secondary">
      提示：耳机库存仅 5 件，最适合观察售罄过程；演示跑完后点「重置演示库存」即可反复玩。
      商品卡片会随 2s 自动刷新同步跳动。
    </p>

    <div v-if="raceStats" class="mb-3">
      <div class="mb-2 flex flex-wrap gap-x-5 gap-y-1 text-sm">
        <span>总请求 <b>{{ raceStats.total }}</b></span>
        <span class="text-success">成功 {{ raceStats.wins }}</span>
        <span class="text-danger">售罄 {{ raceStats.soldOut }}</span>
        <span class="text-primary">限购 {{ raceStats.duplicate }}</span>
        <span class="text-ink-secondary">其他 {{ raceStats.other }}</span>
        <span class="text-ink-secondary">耗时 {{ raceStats.elapsed }}ms · 平均 RT {{ raceStats.avgRt }}ms</span>
      </div>
      <div class="flex h-2 overflow-hidden rounded-full bg-code-bg">
        <div class="bg-success transition-all duration-300" :style="{ width: pct(raceStats.wins) }" />
        <div class="bg-danger transition-all duration-300" :style="{ width: pct(raceStats.soldOut) }" />
        <div class="bg-primary transition-all duration-300" :style="{ width: pct(raceStats.duplicate) }" />
      </div>
      <div
        class="mt-2 text-xs"
        :class="invariantOk === false ? 'text-danger' : 'text-ink-secondary'"
      >
        对账：初始库存 {{ raceInitialStock }} − 成功 {{ raceStats.wins }} = 服务端剩余
        {{ raceServerStock ?? '获取中…' }}
        <template v-if="invariantOk !== null">
          {{ invariantOk ? '—— 不变式成立，零超卖' : '—— 不变式不成立！' }}
        </template>
      </div>
      <p v-if="raceStats.winUsers.length" class="mt-1 text-xs text-ink-secondary">
        抢到的人：{{ raceStats.winUsers.join('、') }}
      </p>
    </div>

    <div
      v-if="raceLogs.length"
      class="max-h-44 overflow-y-auto rounded-md bg-code-bg px-3 py-2 font-mono text-xs leading-5"
    >
      <p v-for="(l, idx) in raceLogs" :key="idx" :class="logClass(l.code)">
        [+{{ l.t }}ms] {{ l.user }} → {{ l.code === 200 ? `抢到了（订单 ${l.orderId}）` : l.msg }} ·
        {{ l.code }} · rt {{ l.rt }}ms
      </p>
    </div>
  </section>

  <section class="rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-3 text-[15px] font-semibold">我的抢购订单</h2>
    <p v-if="!orders.length" class="text-sm text-ink-secondary">还没有抢购成功的订单</p>
    <ul v-else class="space-y-2">
      <li
        v-for="order in orders"
        :key="order.orderId"
        class="flex flex-wrap items-center justify-between gap-2 rounded-md bg-code-bg px-3 py-2 text-sm"
      >
        <span class="font-medium">{{ order.itemName }}</span>
        <span class="text-danger">{{ fmtPrice(order.price) }}</span>
        <span class="rounded-full bg-success/10 px-2 py-0.5 text-xs text-success">已抢购</span>
        <span class="text-xs text-ink-secondary">{{ fmtTime(order.createTime) }}</span>
      </li>
    </ul>
  </section>
</template>
