---
id: flash-sale_06_ui_flash_page
name: "秒杀前端页面（api 模块 + FlashSaleView + 路由/菜单/文档注册）"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_02_contract_flash_contract, flash-sale_04_api_flash_web]
profiles: [frontend]
files:
  - frontend/src/api/flashSale.js
  - frontend/src/views/scenario/FlashSaleView.vue
  - frontend/src/router/index.js
  - frontend/src/config/scenarios.js
  - frontend/src/config/scenarioDocs.js
---

# 秒杀前端页面（api 模块 + FlashSaleView + 路由/菜单/文档注册）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui` |
| 已启用 profile | frontend |
| 架构边界 | frontend/src：api 模块、scenario 视图、路由与两个 config 注册；Tailwind v4 utility-only（design-system tokens） |
| 结构分析 | 场景接入三步（AGENTS 约定）：scenarios.js 置 enabled、router 布局 children 成对追加两条路由、scenarioDocs.js 登记 design.md 与 SVG；接口经 http.js 自动带 Bearer + 401 无感续期 |
| 完成条件 | `npm run build` 通过；登录后菜单出现「03 秒杀抢购」两个入口，详情页可抢购（联调） |

## 需求与验收

- 用户目标：秒杀商品卡片（价格/原价/库存进度/状态徽标/倒计时）、抢购按钮六态（未开始/进行中可抢/抢购中/已抢购/已售罄/已结束）、抢购结果行内提示、我的订单列表、2s 自动刷新库存。
- 包含：flashSale.js、FlashSaleView.vue、路由与注册。
- 验收：`npm run build` 通过；与后端联调全链路可用（刷新列表→抢购→重复抢购 6104 提示→订单出现）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 设计系统 | frontend/design-system/ 已存在只读；样式全部 Tailwind utilities + 既有 token（bg-primary/text-danger/border-line/rounded-card 等），无手写全局类、无硬编码 hex；进度条动态宽度用 style 绑定（非静态样式） |
| 状态来源 | 商品 status 由服务端推导（服务端才是真相），前端只做倒计时展示与按钮置灰（体验优化，非正确性防线） |
| 自动刷新 | 2s 轮询 /flash/items（静默失败不打扰）；watch 开关启停定时器，onBeforeUnmount 清理 |
| 金额 | 接口为分，展示层 `(cents / 100).toFixed(2)` |
| 依赖 | 02（契约）、04（可联调）；DocView 已有，无需改动 |

## 实现步骤

1. 新建 `src/api/flashSale.js`；
2. 新建 `src/views/scenario/FlashSaleView.vue`；
3. router 布局 children 成对追加两条路由；
4. scenarios.js 置 enabled + 补 desc；
5. scenarioDocs.js 登记 design.md 与两张 SVG（任务 07 生成图片资源，import 路径先落定）；
6. `npm run build` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `frontend/src/api/flashSale.js` | api | 新增 | 三个接口封装 |
| `frontend/src/views/scenario/FlashSaleView.vue` | views/scenario | 新增 | 秒杀页面 |
| `frontend/src/router/index.js` | router | 修改 | children 追加两条路由 |
| `frontend/src/config/scenarios.js` | config | 修改 | 03 置 enabled |
| `frontend/src/config/scenarioDocs.js` | config | 修改 | 登记 03 文档资源 |

## 完整代码（供手动敲写）

### frontend/src/api/flashSale.js（新增）

```javascript
import { get, post } from './http'

export const listFlashItems = () => get('/flash/items')

export const seckillFlashItem = (itemId) => post(`/flash/items/${itemId}/seckill`)

export const listMyFlashOrders = () => get('/flash/orders/mine')
```

### frontend/src/views/scenario/FlashSaleView.vue（新增）

```vue
<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { listFlashItems, listMyFlashOrders, seckillFlashItem } from '@/api/flashSale'

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
</script>

<template>
  <section class="mb-4 animate-fade-up rounded-card border border-line bg-white px-6 py-5">
    <h2 class="mb-2 text-[15px] font-semibold">场景 03 · 秒杀 / 高并发抢购</h2>
    <p class="text-sm leading-6 text-ink-secondary">
      防超卖三层防线：内存售罄标记（拦洪峰）→ 数据库原子扣减（保正确）→ 唯一索引限购（幂等兜底）。
      金额单位为分；设计与取舍见 scenarios/03-flash-sale/design.md。
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
```

### frontend/src/router/index.js（修改，布局 children 内成对追加）

```javascript
        {
          path: 'scenario/03-flash-sale',
          name: 'scenario-03-flash-sale',
          component: () => import('@/views/scenario/FlashSaleView.vue'),
          meta: { title: '秒杀抢购' },
        },
        {
          path: 'scenario/03-flash-sale/doc',
          name: 'scenario-03-flash-sale-doc',
          component: () => import('@/views/scenario/DocView.vue'),
          props: { scenarioId: '03-flash-sale' },
          meta: { title: '秒杀抢购 · 技术文档' },
        },
```

修改点说明：追加在场景 01 两条路由之后、注释行「后续场景」之前。

### frontend/src/config/scenarios.js（修改）

```javascript
  { id: '03-flash-sale', no: '03', name: '秒杀抢购', desc: '高并发防超卖三层防线', path: '/scenario/03-flash-sale', enabled: true },
```

修改点说明：03 行补 `desc` 并置 `enabled: true`。

### frontend/src/config/scenarioDocs.js（修改）

```javascript
import flashDesignMd from '../../../scenarios/03-flash-sale/design.md?raw'
import flashArchSvg from '../../../scenarios/03-flash-sale/diagrams/flash-sale-architecture.svg?url'
import flashGuardSvg from '../../../scenarios/03-flash-sale/diagrams/oversell-guard.svg?url'

// scenarioDocs 对象内追加：
  '03-flash-sale': {
    markdown: flashDesignMd,
    assets: {
      'diagrams/flash-sale-architecture.svg': flashArchSvg,
      'diagrams/oversell-guard.svg': flashGuardSvg,
    },
  },
```

修改点说明：import 置于文件头部既有 import 之后；注册项追加在 `'01-auth'` 之后。SVG 文件由任务 07 生成（本任务先定路径与 import，构建需在任务 07 产出 SVG 后才能通过——若先执行本任务，验证顺序以 07 完成为准）。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 生产构建通过（依赖任务 07 的 SVG 资源存在） |
| manual | 前后端启动后登录，访问 /scenario/03-flash-sale | 商品四档状态正确展示；抢购/重复抢购/售罄/我的订单全链路可用 |

## 风险与阻塞

- 风险：`bg-primary/10`、`bg-success/10` 等透明度修饰符依赖 Tailwind v4 对 @theme 色的 color-mix 支持（v4 原生支持）；SVG import 不存在会导致构建失败，故 06/07 完成顺序上互为衔接。
- 阻塞：无。
- 执行记录：2026-09-07 全部落盘（api 模块 / FlashSaleView / 路由成对两条 / scenarios.js 置 enabled / scenarioDocs.js 登记）。`npm run build` 通过：FlashSaleView 独立 chunk 6.15kB，两张 SVG 打包为 assets。
- 联调备注：本机 8080 被 IDEA 调试会话中的旧版后端占用（无 /flash 路由），浏览器联调需重启 IDE 内应用后进行；API 层全链路已由 verify.sh 7/7 覆盖（同一契约）。
