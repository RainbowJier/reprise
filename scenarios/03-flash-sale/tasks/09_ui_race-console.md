---
id: flash-sale_09_ui_race_console
name: "并发演示控制台（浏览器端洪峰 + 实时统计/日志/对账）"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_06_ui_flash_page, flash-sale_08_api_demo_reset]
profiles: [frontend]
files:
  - frontend/src/api/flashRace.js
  - frontend/src/api/flashSale.js
  - frontend/src/views/scenario/FlashSaleView.vue
  - scenarios/03-flash-sale/NOTES.md
  - AGENTS.md
---

# 并发演示控制台（浏览器端洪峰 + 实时统计/日志/对账）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui`（增量，用户需求：前端体现并发全过程） |
| 已启用 profile | frontend |
| 架构边界 | 新增 `api/flashRace.js`（洪峰专用裸 axios）；FlashSaleView 增「并发抢购演示」区块 |
| 完成条件 | `npm run build` 通过；浏览器实测两种模式的统计、日志、对账与重置循环 |

## 需求与验收

- 用户目标：页面上可观察并发抢购全过程——请求洪峰、库存实时扣减、售罄拒绝、限购兜底、最终对账。
- 两模式：**多用户洪峰**（注册 N 个虚拟用户同时开抢，随机 0~400ms 起跑差）；**单用户连点**（当前登录用户 12 连击）。
- 验收：统计分布（成功/售罄/限购/其他 + 耗时/平均 RT）、逐请求彩色日志流（最近 100 条）、
  服务端对账行「初始库存 − 成功 = 服务端剩余 —— 不变式成立」、抢到的人名单、
  重置按钮一键循环；商品卡片与 2s 自动刷新同步跳动。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| token 隔离 | http.js 拦截器会无条件覆盖 Authorization 头，洪峰用户走 `flashRace.js` 独立裸 axios 实例，与登录态彻底隔离 |
| 注册节流 | 分批 8 并发注册（BCrypt ~80ms/次），24 用户约 1s；用户名 `race${Date.now()}_k`（仅字母数字下划线） |
| 结果分类 | `raceSeckill` 永不 reject：200/6103/6104/-1（HTTP 层异常）都归入竞速结果；RT 用 performance.now() |
| 对账语义 | 洪峰结束后拉 `/flash/items` 取服务端剩余，前端断言 `初始 − 成功 == 剩余`；连点模式顺带刷新「我的订单」（当前用户可能抢到） |
| 依赖 | 06（页面基座）、08（重置接口） |

## 完整代码

### frontend/src/api/flashRace.js（新增，全量）

```javascript
// 并发演示专用裸 axios 实例：不走 http.js 拦截器——
// 洪峰虚拟用户各有独立 token，不能复用当前登录态的请求头注入与 401 续期逻辑
import axios from 'axios'

const raceHttp = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

/**
 * 注册一个洪峰虚拟用户（注册即登录，直接返回 token）。
 */
export async function registerRaceUser(username) {
  const { data: body } = await raceHttp.post('/auth/register', {
    username,
    password: 'pass123456',
  })
  if (body?.code !== 200) {
    throw new Error(body?.msg || `注册并发用户失败（code=${body?.code}）`)
  }
  return { username, accessToken: body.data.accessToken }
}

/**
 * 单次抢购请求。竞速结果本身就是数据：HTTP 层异常也不 reject，统一归入结果码。
 *
 * @returns {{code: number, msg: string, orderId: string|null, rt: number}}
 */
export async function raceSeckill(itemId, accessToken) {
  const started = performance.now()
  const rt = () => Math.round(performance.now() - started)
  try {
    const { data: body } = await raceHttp.post(
      `/flash/items/${itemId}/seckill`,
      {},
      { headers: { Authorization: `Bearer ${accessToken}` } },
    )
    return {
      code: body?.code ?? -1,
      msg: body?.msg ?? '',
      orderId: body?.data?.orderId ?? null,
      rt: rt(),
    }
  } catch (error) {
    return { code: -1, msg: error.message, orderId: null, rt: rt() }
  }
}
```

### frontend/src/api/flashSale.js（修改，文件末尾追加）

```javascript
// 演示专用：重置商品库存与订单（仅进行中活动；使并发演示可反复运行）
export const resetFlashDemoItem = (itemId) => post(`/flash/demo/reset/${itemId}`)
```

### frontend/src/views/scenario/FlashSaleView.vue（修改）

修改点（以最终落盘文件为准，此处列增量要点）：

1. import 增：`getAccessToken`（@/api/authTokens）、`resetFlashDemoItem` 并入 flashSale 导入、`raceSeckill/registerRaceUser`（@/api/flashRace）；
2. script 末尾追加「并发抢购演示」状态与逻辑（RACE_MODES/BURST_TIMES=12/REGISTER_BATCH=8、raceMode/raceUsers/raceItemId/racePhase/raceStats/raceLogs/raceInitialStock/raceServerStock、raceBusy/racableItems/raceTarget/canStartRace/invariantOk 计算、items watch 默认选耳机、startRace 两模式编排、resetRaceItem、logClass/pct/sleep 辅助）；`startRace` 收尾 `Promise.all([loadItems(true), loadOrders()])` 保证连点模式订单区刷新；
3. template「秒杀商品」与「我的订单」之间插入「并发抢购演示」section：模式切换（多用户洪峰/单用户连点）、并发用户数（4–50）、目标商品下拉（含实时库存）、开始/重置按钮、结果统计行 + 三色比率条（success/danger/primary）、对账行（不变式断言文本）、抢到的人、等宽字体日志流（max-h-44 滚动，颜色按结果码）。

## 验证

| 验证方式 | 命令或步骤 | 结果 |
|----------|------------|------|
| build | `cd frontend && npm run build` | 通过（FlashSaleView chunk 13.9kB） |
| browser | dev server（白名单端口）+ 真实后端，浏览器驱动两种模式 | 洪峰：24 用户→5 成功/19 售罄，1511ms/平均 RT 44ms，日志 RT 23→62ms 递增（行锁排队），对账不变式成立；连点：12 连击→1 成功+11×6104，复跑 12×6104，订单区自动刷新；重置一键循环 |

## 风险与阻塞

- 风险：洪峰用户数上限 50（Tomcat 200 工作线程内）；联调端口必须在 CORS 白名单（5173/5174），临时端口会 403。
- 阻塞：无。
- 执行记录：2026-09-07 落盘；浏览器实测期间发现连点模式订单区不刷新，收尾补 `loadOrders()` 后复验通过；IAB 环境点击派发受限，以页面上下文 evaluate 驱动 + DOM 断言完成验证。
