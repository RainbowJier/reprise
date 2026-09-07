# 03. 秒杀 / 高并发抢购

- **一句话**：高并发下防超卖的最小正确实现——内存闸门拦洪峰、数据库原子扣减保正确、唯一索引限购兜底。
- **日期**：2026-09
- **技术栈**：Spring Boot 3.5（MyBatis-Plus @Update 原生 SQL + @Transactional）/ Vue 3 + Tailwind v4 / H2

## 1. 需求

瞬时洪峰 × 有限库存：开抢瞬间绝大多数请求注定失败，系统要在洪峰里筛出极少数成功者，
且绝不超卖（库存扣成负数）也不少卖（有库存却全拒）。核心链路：浏览列表（实时库存/状态）
→ 到点抢购（校验→扣库存→建订单，三者原子）→ 查询我的订单。

## 2. 方案设计与取舍

- 数据模型：`flash_items`（total_stock/stock 双字段，前者是对账基准）+ `flash_orders`
  （`uk(item_id, user_id)` 唯一索引限购；item_name/price 下单快照）。
- 选型（零新增三方依赖，本机 Maven 内网镜像不可达也是现实约束）：
  - DB 原子条件更新 vs Redis 预扣：单库方案先用对再上快——`UPDATE ... SET stock = stock - 1
    WHERE stock > 0` 一条语句保证正确性，Redis 列入延伸（场景 09/10）；
  - 原子 UPDATE vs `SELECT FOR UPDATE` 悲观锁：悲观锁在事务持锁期间串行阻塞、占满连接池，
    条件更新锁粒度最小、持锁最短；
  - 先扣库存再插订单 + 同事务回滚：插订单撞唯一索引则扣减一并回滚，「扣了没单」「有单没扣」都不允许；
  - 下单即扣库存 vs 支付扣库存：后者会被「下单占位不付款」黄牛占坑；本场景无支付，下单即成交；
  - 错误码 61xx 分段（6101 未开始/6102 已结束/6103 已售罄/6104 重复抢购）：秒杀的失败大多是
    正常业务结果，不与 HTTP 语义码混用，前端按 code!==200 统一展示 msg；
  - 金额一律 BIGINT 存分，不用 DECIMAL/浮点序列化。
- 架构：请求漏斗五层（前端置灰→JWT 过滤器→内存售罄标记→原子扣减→唯一索引），
  每层把无效流量拦向更外面；内存标记只做性能不做正确性（丢了标记多查几次库，不会超卖）。
  `/flash/*` 纳入场景 01 JwtAuthFilter 的 patterns（AuthFilterConfig 预留的扩展点）。

## 3. 实现要点

- `FlashItemMapper.deductStock` 用 `@Update` 原生 SQL：判断（stock>0）与扣减同语句原子完成，
  调用方只认影响行数、不认先前查到的任何库存值；`update_time` 在 SQL 里一并维护
  （wrapper 的 setSql 更新不触发 MetaObjectHandler 自动填充）；
- `seckill` 加 `@Transactional(rollbackFor = Exception.class)`：DuplicateKeyException 转 6104
  的同时整个事务回滚、库存恢复——事务边界就是「扣减与下单同生共死」的边界；
- `findByItemAndUser` 前置查重只是优化（减少无效扣减与行锁竞争），并发下仍可能同时通过查重，
  唯一索引才是物理兜底——优化与兜底的层次不能混；
- 集成测试并发用户不走注册接口（BCrypt 一次约 80ms，200 用户约 15s）：
  `UserGateway.insert` 直插 + `TokenProvider.issueToken` 直接签发，构造保持毫秒级；
- 前端：服务端 status 为准（服务端才是真相），倒计时/置灰只是体验层；抢购失败静默刷新列表
  （多为刚售罄）；自动刷新开关 2s 轮询库存；分→元只在展示层换算；
- 并发演示控制台（增量）：洪峰虚拟用户走 `flashRace.js` 独立裸 axios 实例——http.js 的拦截器
  会无条件覆盖 Authorization 头，虚拟用户的 token 必须与当前登录态隔离；注册分批 8 并发
  （BCrypt ~80ms/次），开抢加随机 0~400ms 起跑差让日志流可读也更接近真实点击分布；
  演示循环依赖后端重置接口三件套：库存回满 + 订单物理删除（逻辑删除不释放唯一索引！）+ 售罄标记清除。

## 4. 踩坑记录

| 现象 | 原因 | 解决 |
| ---- | ---- | ---- |
| 冒烟并发演示「成功 4 售罄 8」，脚本判失败 | 8080/8081 上跑的是旧实例（库存已被上轮消耗为 4），非超卖——不变式其实成立（4 单恰好吃光库存 4） | 冒烟改用新注册主用户 + 固定期望值注明「重复运行需重启后端」；发现 `kill %1` 在跨 Bash 调用的脚本里无效（job 不持久），必须 netstat 查 PID 后 taskkill |
| 旧实例访问 /flash/items 返回 code=500 而非 404 | 8080 是 IDEA 调试会话里的旧版 DemoApplication（无 /flash 路由），Spring 的 No static resource 被 GlobalExceptionHandler 的 Exception 兜底转成 500 | 本机验证走 8081 + verify.sh 支持 FLASH_BASE 覆盖；要恢复 8080 需重启 IDE 里的应用 |
| 冒烟注册并发用户失败 | 用户名 `race-…` 含连字符，被 RegisterReq 的 `^[A-Za-z0-9_]{4,32}$` 417 拒绝 | 改 `race${STAMP}_$i`（下划线）；跨场景复用账号规则时先看对方契约 |
| 集成测试编译错「String 无法转换为 int」 | 对 `List<Map>` 调了 `.get("accessToken")`——List.get 只收 int 索引；登录响应 data 是对象、列表接口 data 才是数组 | 拆出 data()/dataList() 两个辅助方法，按响应形状取用 |
| 临时联调用 5189 端口，页面登录 POST 全挂 | 5189 不在 `app.cors.allowed-origins` 白名单，代理 POST 被 CORS 403「Invalid CORS request」；GET /health 不触发预检所以「看起来正常」，POST 才暴露（AGENTS 已记载的坑的活例子） | 联调端口只用白名单内的 5173/5174；排查时从页面上下文 fetch 探针直接看到 403 文本 |
| 浏览器实测时 IAB 内 Playwright click 超时、截图被拒 | 疑似 IAB guest 的输入派发限制（fill 正常、click 不落） | 页面上下文 `evaluate` 派发 `btn.click()` 等效触发 Vue @click，DOM 状态断言代替截图；不影响功能本身的验证结论 |
| 6103/6104 在日志里打 ERROR 级别并带堆栈 | GlobalExceptionHandler 的 isClientError 只认 400-499，61xx ≥ 500 被当服务端错误 | 已知取舍：61xx 是行业惯用分段，日志噪音留待 common 层为业务码段扩日志级别规则时一并处理 |
| H2 种子要「进行中/未开始/已结束」四档状态随时可演示 | 固定绝对时间重启后就过期 | `CURRENT_TIMESTAMP ± INTERVAL` 相对生成（H2 方言）；PostgreSQL 手工脚本用 `now() ± interval` |

## 5. 验证与数据

- 后端 `mvn test` 22/22：场景 01 回归 10 + contextLoads + 场景 03 共 11 用例（含 Order(10)/(11) 演示重置：
  重置后库存回满、同用户可复购、售罄标记清除；已结束活动重置被拒 417）；
- 并发防超卖（Order 8）：200 个互不相同用户 CountDownLatch 同抢库存 5 的商品——
  成功恰 5、失败 195 且全部 6103、终态库存 0，对账式不变式「订单数 + 库存 == 初始库存」成立；
- 单用户幂等（Order 9）：同一 token 20 线程并发抢同一商品——恰 1 单成功、19 次 6104；
- curl 冒烟 `verify.sh` 9/9 × 2 遍（同一后端连跑，验证第 8 步重置使脚本可重复运行）；
- 浏览器端并发演示控制台实测（Vite dev + H2 后端）：
  - 多用户洪峰：24 个虚拟用户抢 5 件库存 → 成功 5 / 售罄 19，耗时 1511ms、平均 RT 44ms，
    日志可见 RT 随行锁排队从 23ms 递增至 62ms，页面同步显示「已抢 5/5 + 已售罄」与对账不变式成立；
  - 单用户连点：12 连击 → 恰 1 成功 + 11×6104（53ms）；复跑 12×6104（限购持续生效），
    「我的订单」竞速后自动刷新；
  - 重置演示库存：一键回满、按钮复活、可无限循环演示；
- 未做真实压测（QPS/RT 数据）：瓶颈预判在 H2 单行热点更新与 Hikari 默认 10 连接，
  秒杀语义正确性已由并发不变式覆盖；真实压测列入延伸。

## 6. 参考与延伸

- 延伸：Redis 预扣（Lua 原子 decr + 异步落账）· MQ 异步下单削峰 · 令牌桶限流 + 验证码打散 ·
  订单超时取消回补库存（联动标记清除）· 分布式锁与库存分桶（场景 09）· 多级缓存（场景 10）· 支付对账（场景 07）
