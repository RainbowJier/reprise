---
id: flash-sale_05_test_flash_concurrency
name: "并发防超卖测试（集成测试 + verify.sh 冒烟）"
type: test
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_04_api_flash_web]
profiles: [backend, testing]
files:
  - backend/demo/demo-starter/src/test/java/com/fullstack/demo/starter/FlashSaleIntegrationTest.java
  - scenarios/03-flash-sale/verify.sh
---

# 并发防超卖测试（集成测试 + verify.sh 冒烟）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `test` |
| 已启用 profile | backend / testing |
| 架构边界 | demo-starter test（黑盒 HTTP 集成测试，TestRestTemplate 自动携带 context-path）；scenarios/03-flash-sale/verify.sh（运行中后端的 curl 冒烟） |
| 结构分析 | 沿用场景 01 AuthFlowIntegrationTest 模式（RANDOM_PORT + 随机 H2 + data.sql 种子）；并发用户新增不走注册接口（BCrypt 太慢），直接 UserGateway 插入 + TokenProvider 签发 token |
| 完成条件 | `mvn test` 全部通过：含 200 用户并发抢 5 库存零超卖、单用户并发重复提交仅 1 单 |

## 需求与验收

- 用户目标：用并发制造竞态窗口，断言不变式而非单次结果。
- 包含：FlashSaleIntegrationTest（9 个用例）+ verify.sh（7 步冒烟，含 shell 并发演示）。
- 验收：
  - 并发 200 用户抢 stock=5 商品 → 成功恰好 5、失败全部 6103、终态库存 0（订单数 + 库存 == 初始库存）；
  - 单用户 20 线程并发重复抢购 → 恰好 1 单，其余 6104；
  - 状态边界（6101/6102/404）、认证边界（401）、功能链路（列表/我的订单）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 测试用户构造 | `userGateway.insert`（固定哈希占位，不走 HTTP 注册）+ `tokenProvider.issueToken` 直接签发 access token——微秒级，规避 BCrypt 注册 200 次（约 15s）的耗时 |
| 并发模型 | CountDownLatch 对齐起跑 + ExecutorService 固定线程池；TestRestTemplate 线程安全。Tomcat 默认 200 工作线程、Hikari 默认 10 连接：并发语义由数据库单行原子更新保证，与连接排队无关（这正是原子条件更新优于应用层判断的意义） |
| 不变式 | 任意并发下 `成功订单数 + 剩余库存 == 初始库存`；成功订单的用户互不相同（限购） |
| shell 并发 | verify.sh 用 12 个注册用户后台 curl 并发抢 4 件库存（demo 已抢 1 件），期望恰好 4 成功 8 售罄；载荷保持 ASCII（Windows 控制台编码坑，见场景 01） |
| 依赖 | 04（接口可调）；被 07（文档引用验证数据）依赖 |

## 实现步骤

1. starter test 新建 FlashSaleIntegrationTest；
2. 新建 scenarios/03-flash-sale/verify.sh；
3. `cd backend && mvn test`；
4. 本地启动后端跑 verify.sh 冒烟。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-starter/src/test/java/com/fullstack/demo/starter/FlashSaleIntegrationTest.java` | starter test | 新增 | 黑盒集成测试 |
| `scenarios/03-flash-sale/verify.sh` | 场景目录 | 新增 | curl 冒烟脚本 |

## 完整代码（供手动敲写）

### backend/demo/demo-starter/src/test/java/com/fullstack/demo/starter/FlashSaleIntegrationTest.java（新增）

```java
package com.fullstack.demo.starter;

import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 场景 03 秒杀集成测试（黑盒 HTTP，随机 H2 + data.sql 种子）。
 * <p>
 * 并发正确性断言的是不变式而非单次结果：任意并发下
 * 成功订单数 + 剩余库存 == 初始库存，且成功订单用户互不相同。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlashSaleIntegrationTest {

    /** 并发用户数（= 线程数，Tomcat 默认 200 工作线程内） */
    private static final int CONCURRENT_USERS = 200;

    /** 单用户并发重复提交线程数 */
    private static final int DUPLICATE_THREADS = 20;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private TokenProvider tokenProvider;

    private Map<String, Object> post(String path, Object body, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return rest.exchange(path, HttpMethod.POST, entity, Map.class).getBody();
    }

    private Map<String, Object> get(String path, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        if (bearer != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> dataList(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private String loginDemo() {
        Map<String, Object> body = post("/auth/login",
                Map.of("username", "demo", "password", "demo123456"), null);
        return (String) data(body).get("accessToken");
    }

    /**
     * 直接插入测试用户并签发 access token：绕开注册接口的 BCrypt（约 80ms/次），
     * 200 个用户的构造保持毫秒级。
     */
    private String tokenForNewUser(String suffix) {
        User user = new User();
        user.setUsername("flash-" + System.nanoTime() + "-" + suffix);
        user.setPassword("$2a$10$placeholder-not-for-login");
        user.setNickname("并发测试用户");
        userGateway.insert(user);
        TokenResp token = tokenProvider.issueToken(user.getId(), user.getUsername(), user.getNickname());
        return token.getAccessToken();
    }

    @Test
    @Order(1)
    void flash_without_token_returns_401() {
        Map<String, Object> body = get("/flash/items", null);
        assertThat(body.get("code")).isEqualTo(401);
    }

    @Test
    @Order(2)
    void list_items_returns_seeded_states() {
        List<Map<String, Object>> items = dataList(get("/flash/items", loginDemo()));
        assertThat(items).hasSize(4);
        // 种子固定：1/2 进行中、3 未开始、4 已结束；demo 用户尚未抢购
        assertThat(items).anySatisfy(i -> {
            assertThat(i.get("id")).isEqualTo(1);
            assertThat(i.get("status")).isEqualTo("IN_PROGRESS");
            assertThat(i.get("mine")).isEqualTo(false);
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.get("id")).isEqualTo(3);
            assertThat(i.get("status")).isEqualTo("NOT_STARTED");
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.get("id")).isEqualTo(4);
            assertThat(i.get("status")).isEqualTo("ENDED");
        });
    }

    @Test
    @Order(3)
    void seckill_not_started_returns_6101() {
        Map<String, Object> body = post("/flash/items/3/seckill", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(6101);
    }

    @Test
    @Order(4)
    void seckill_ended_returns_6102() {
        Map<String, Object> body = post("/flash/items/4/seckill", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(6102);
    }

    @Test
    @Order(5)
    void seckill_nonexistent_returns_404() {
        Map<String, Object> body = post("/flash/items/999/seckill", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(404);
    }

    @Test
    @Order(6)
    void seckill_success_then_duplicate_returns_6104() {
        String token = loginDemo();
        Map<String, Object> first = post("/flash/items/1/seckill", Map.of(), token);
        assertThat(first.get("code")).isEqualTo(200);
        assertThat(data(first)).isNotNull();
        assertThat(data(first).get("orderId")).isNotNull();

        Map<String, Object> second = post("/flash/items/1/seckill", Map.of(), token);
        assertThat(second.get("code")).isEqualTo(6104);

        // 库存 50 -> 49，且列表标记 mine=true
        List<Map<String, Object>> items = dataList(get("/flash/items", token));
        Map<String, Object> item1 = items.stream().filter(i -> i.get("id").equals(1)).findFirst().orElseThrow();
        assertThat(item1.get("stock")).isEqualTo(49);
        assertThat(item1.get("mine")).isEqualTo(true);
    }

    @Test
    @Order(7)
    void my_orders_contains_success_order() {
        List<Map<String, Object>> orders = dataList(get("/flash/orders/mine", loginDemo()));
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).get("itemId")).isEqualTo(1);
        assertThat(orders.get(0).get("itemName")).isEqualTo("旗舰手机 Pro");
    }

    @Test
    @Order(8)
    void concurrent_users_never_oversell() throws Exception {
        // 商品 2：库存 5；CONCURRENT_USERS 个互不相同用户同时抢
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        ConcurrentHashMap<String, Integer> failureCodes = new ConcurrentHashMap<>();
        try {
            IntStream.range(0, CONCURRENT_USERS).forEach(i -> pool.submit(() -> {
                String token = tokenForNewUser(String.valueOf(i));
                ready.countDown();
                try {
                    start.await();
                    Map<String, Object> body = post("/flash/items/2/seckill", Map.of(), token);
                    int code = ((Number) body.get("code")).intValue();
                    if (code == 200) {
                        success.incrementAndGet();
                    } else {
                        failureCodes.merge(String.valueOf(code), 1, Integer::sum);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        } finally {
            pool.shutdown();
        }
        // awaitTermination 必须在 shutdown() 之后调用，否则立即返回 false
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // 不变式：成功订单数恰为初始库存 5；其余全部 6103（互不相同用户不存在 6104）
        assertThat(success.get()).isEqualTo(5);
        assertThat(failureCodes).containsOnly(Map.entry("6103", CONCURRENT_USERS - 5));

        // 终态库存为 0（对账：订单数 + 库存 == 初始库存）
        List<Map<String, Object>> items = dataList(get("/flash/items", loginDemo()));
        Map<String, Object> item2 = items.stream().filter(i -> i.get("id").equals(2)).findFirst().orElseThrow();
        assertThat(item2.get("stock")).isEqualTo(0);
    }

    @Test
    @Order(9)
    void concurrent_duplicate_submissions_create_single_order() throws Exception {
        // 同一用户 DUPLICATE_THREADS 线程并发抢购同一商品：唯一索引保证恰好 1 单
        String token = tokenForNewUser("dup");
        ExecutorService pool = Executors.newFixedThreadPool(DUPLICATE_THREADS);
        CountDownLatch ready = new CountDownLatch(DUPLICATE_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();
        try {
            IntStream.range(0, DUPLICATE_THREADS).forEach(i -> pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    Map<String, Object> body = post("/flash/items/1/seckill", Map.of(), token);
                    int code = ((Number) body.get("code")).intValue();
                    if (code == 200) {
                        success.incrementAndGet();
                    } else if (code == 6104) {
                        duplicate.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        } finally {
            pool.shutdown();
        }
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(1);
        assertThat(duplicate.get()).isEqualTo(DUPLICATE_THREADS - 1);
    }
}
```

### scenarios/03-flash-sale/verify.sh（新增）

```bash
#!/usr/bin/env bash
# 场景 03 秒杀抢购 curl 冒烟：需后端运行于 localhost:8080（H2 默认配置，种子数据随启动生成）
# 用法：bash scenarios/03-flash-sale/verify.sh
# 可用 FLASH_BASE 覆盖目标地址（默认 8080；例如本机 8080 被其他实例占用时）：
#   FLASH_BASE=http://localhost:8081/api bash scenarios/03-flash-sale/verify.sh
# 注意：
#   1. 请求体保持 ASCII（Windows 控制台编码会把中文载荷损坏成非法 JSON）
#   2. 第 6 步并发演示会耗尽耳机（id=2）全部库存且不可恢复——重复运行前请重启后端
set -euo pipefail

BASE="${FLASH_BASE:-http://localhost:8080/api}"
STAMP="$(date +%s)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

json_field() {
  # 从 stdin JSON 提取字段：json_field data.accessToken
  python -c "import sys,json; d=json.load(sys.stdin); [d:=d.get(k) if isinstance(d,dict) else None for k in sys.argv[1].split('.')]; print(d if d is not None else '')" "$1"
}

fail() { echo "冒烟失败：$1" >&2; exit 1; }

step() { echo; echo "=== $1 ==="; }

step "1. 注册冒烟主用户（注册即登录）"
REG=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"smoke${STAMP}\",\"password\":\"pass123456\"}")
ACCESS=$(echo "$REG" | json_field data.accessToken)
if [ -z "$ACCESS" ]; then fail "注册失败：$REG"; fi
echo "注册成功，access=${ACCESS:0:8}…"

step "2. 秒杀商品列表（期望 4 件，状态四档齐备）"
ITEMS=$(curl -sf "$BASE/flash/items" -H "Authorization: Bearer $ACCESS")
COUNT=$(echo "$ITEMS" | python -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))")
if [ "$COUNT" != "4" ]; then fail "期望 4 件商品，实际 $COUNT：$ITEMS"; fi
STATUS_CHECK=$(echo "$ITEMS" | python -c "
import sys, json
items = {i['id']: i for i in json.load(sys.stdin)['data']}
expect = {1: 'IN_PROGRESS', 2: 'IN_PROGRESS', 3: 'NOT_STARTED', 4: 'ENDED'}
print('ok' if all(items[k]['status'] == v for k, v in expect.items()) else 'bad')")
if [ "$STATUS_CHECK" != "ok" ]; then fail "状态不符：$ITEMS"; fi
STOCK2=$(echo "$ITEMS" | python -c "import sys,json; d=json.load(sys.stdin); print([i['stock'] for i in d['data'] if i['id']==2][0])")
echo "4 件商品状态正确，耳机（id=2）当前库存 $STOCK2"

step "3. 抢购旗舰手机（期望成功）"
ORDER=$(curl -sf -X POST "$BASE/flash/items/1/seckill" -H "Authorization: Bearer $ACCESS")
ORDER_ID=$(echo "$ORDER" | json_field data.orderId)
if [ -z "$ORDER_ID" ]; then fail "抢购未返回订单：$ORDER"; fi
echo "抢购成功，订单号 $ORDER_ID"

step "4. 重复抢购（期望 code=6104）"
CODE=$(curl -s -X POST "$BASE/flash/items/1/seckill" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE" != "6104" ]; then fail "期望 6104 重复抢购，实际 $CODE"; fi
echo "限购兜底生效（6104）✓"

step "5. 状态边界：未开始 6101 / 已结束 6102"
CODE3=$(curl -s -X POST "$BASE/flash/items/3/seckill" -H "Authorization: Bearer $ACCESS" | json_field code)
CODE4=$(curl -s -X POST "$BASE/flash/items/4/seckill" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE3" != "6101" ] || [ "$CODE4" != "6102" ]; then
  fail "期望 6101/6102，实际 $CODE3/$CODE4"
fi
echo "未开始 6101 ✓  已结束 6102 ✓"

step "6. 并发演示：12 个用户并发抢耳机 5 件库存（期望恰好 5 成功 7 售罄）"
TOKENS=()
for i in $(seq 1 12); do
  T=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
    -d "{\"username\":\"race${STAMP}_$i\",\"password\":\"pass123456\"}" | json_field data.accessToken)
  if [ -z "$T" ]; then fail "注册并发用户 $i 失败"; fi
  TOKENS+=("$T")
done
for i in $(seq 1 12); do
  (curl -s -X POST "$BASE/flash/items/2/seckill" -H "Authorization: Bearer ${TOKENS[$((i-1))]}" \
    | json_field code > "$TMP/race-$i") &
done
wait
WINS=$(grep -l '^200$' "$TMP"/race-* | wc -l)
SOLD=$(grep -l '^6103$' "$TMP"/race-* | wc -l)
echo "并发结果：成功 $WINS 单 / 售罄拒绝 $SOLD 单"
if [ "$WINS" != "5" ] || [ "$SOLD" != "7" ]; then
  fail "期望恰好 5 成功 7 售罄（零超卖），实际 $WINS/$SOLD（若重复运行请先重启后端）"
fi
FINAL=$(curl -sf "$BASE/flash/items" -H "Authorization: Bearer $ACCESS" \
  | python -c "import sys,json; d=json.load(sys.stdin); print([i['stock'] for i in d['data'] if i['id']==2][0])")
if [ "$FINAL" != "0" ]; then fail "售罄后库存应为 0，实际 $FINAL"; fi
echo "零超卖对账通过：5 单 + 0 库存 == 初始 5 库存 ✓"

step "7. 我的订单（主用户应含旗舰手机订单）"
MINE=$(curl -sf "$BASE/flash/orders/mine" -H "Authorization: Bearer $ACCESS")
N=$(echo "$MINE" | python -c "import sys,json; d=json.load(sys.stdin); print(sum(1 for o in d['data'] if o['itemId']==1))")
if [ "$N" != "1" ]; then fail "期望主用户有 1 笔旗舰手机订单，实际 $N：$MINE"; fi
echo "订单查询正确 ✓"

echo
echo "全部冒烟步骤通过 ✅"
```


## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| test | `cd backend && mvn test` | 全部通过（场景 01 + 场景 03 共 20 个用例） |
| smoke | 启动后端后 `bash scenarios/03-flash-sale/verify.sh` | 7 步全部通过，含并发零超卖对账 |

## 风险与阻塞

- 风险：`pool.awaitTermination` 必须在 `shutdown()` 之后调用（先 await 后 shutdown 会立即返回 false）；并发断言依赖不变式而非时序，理论上不 flaky；H2 单行热点更新在 200 并发下仅为毫秒级行锁等待。
- 阻塞：无。
- 执行记录：2026-09-07 集成测试与冒烟脚本落盘。`mvn test` 20/20 通过（场景 03 共 9 用例：Order(8) 200 用户并发抢 5 库存恰 5 成功/195 全部 6103/终态库存 0；Order(9) 单用户 20 线程并发恰好 1 单）。冒烟 `FLASH_BASE=http://localhost:8081/api bash scenarios/03-flash-sale/verify.sh` 7/7 通过（12 用户并发抢 5 库存恰 5 成功 7 售罄）。
- 执行期修订：① 测试辅助 loginDemo 误对 List 调 `get("accessToken")`（List.get 只收 int），补 data() 辅助方法；② 抢购响应 data 是对象非数组，Order(6) 断言改用 data()；③ 冒烟注册用户名含连字符被 RegisterReq 的 Pattern（仅字母数字下划线）417 拒绝，改 `race${STAMP}_$i`；④ 冒烟主用户由 demo 改为新注册用户（demo 可能已有历史订单导致 6104 而非成功），并在并发步骤注明「重复运行需重启后端」（竞速消耗库存不可逆）。
- 环境备注：本机 8080 被 IDEA 调试会话中的旧版 DemoApplication 占用（无 /flash 路由），冒烟走 8081 新实例 + FLASH_BASE 覆盖；shell 后台任务跨 Bash 调用不持久，`kill %1` 无效需按 PID taskkill。
