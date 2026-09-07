---
id: flash-sale_08_api_demo_reset
name: "演示重置接口（库存回满 + 订单物理清理 + 售罄标记清除）"
type: api
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_04_api_flash_web]
profiles: [backend, api, database]
files:
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/gateway/FlashItemGateway.java
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/gateway/FlashOrderGateway.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/FlashItemMapper.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/FlashOrderMapper.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/FlashItemGatewayImpl.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/FlashOrderGatewayImpl.java
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/flash/FlashSaleGuard.java
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/flash/FlashSaleServiceImpl.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/FlashSaleService.java
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/FlashSaleController.java
  - backend/demo/demo-starter/src/test/java/com/fullstack/demo/starter/FlashSaleIntegrationTest.java
  - scenarios/03-flash-sale/verify.sh
  - scenarios/03-flash-sale/design.md
---

# 演示重置接口（库存回满 + 订单物理清理 + 售罄标记清除）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `api`（增量，用户需求：前端体现并发全过程 → 演示需可循环） |
| 已启用 profile | backend / api / database |
| 架构边界 | Gateway 端口与实现各加一个方法；Guard 加清除方法；Service/Controller 增 `resetDemoItem`；verify.sh 增第 8/9 步 |
| 完成条件 | `mvn test` 22/22（含 2 个新重置用例）；verify.sh 两遍连跑 9/9 |

## 需求与验收

- 用户目标：前端并发演示可反复运行——重置必须同时做三件事，缺一不可：
  ① 库存回满 `total_stock`；② **物理**删除该商品订单（逻辑删除不释放唯一索引，用户将永远 6104）；③ 清除内存售罄标记（只进不出，不清则重置后全部 6103）。
- 验收：重置后同用户可再次抢购成功；非进行中活动重置 417。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 安全语义 | 演示专用运维入口，任何登录用户可调用；design.md「安全边界」与 AGENTS.md 均已注明生产必须下线/加管理权限 |
| 事务 | `@Transactional`：回满与清订单同事务；状态守卫（仅进行中）在校验段 |
| 依赖 | 04（Controller 既有）；被 09（前端演示控制台）依赖 |

## 完整代码（修改点，落点：各文件既有成员之后追加）

### FlashItemGateway（domain）追加

```java
    /**
     * 演示专用：库存回满到初始值 total_stock（配合订单清理与售罄标记清除使用）。
     */
    void resetStock(Long itemId);
```

### FlashItemMapper 追加

```java
    /**
     * 演示专用：库存回满到初始值（生产环境没有「把库存加回去」这种运维入口，
     * 对应的是补货/回滚等专门流程）。
     */
    @Update("UPDATE flash_items SET stock = total_stock, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{itemId} AND deleted = 0")
    int resetStock(@Param("itemId") Long itemId);
```

### FlashOrderGateway（domain）追加

```java
    /**
     * 演示专用：物理删除某商品的全部订单——逻辑删除不会释放
     * uk(item_id, user_id) 唯一索引，重置后用户将无法再次抢购。
     */
    void deleteByItemId(Long itemId);
```

### FlashOrderMapper 追加

```java
    /**
     * 演示专用：物理删除。必须用原生 DELETE 而不是 MP 的逻辑删除——
     * 逻辑删除只置 deleted=1，唯一索引仍占用，用户重置后将永远 6104。
     */
    @Delete("DELETE FROM flash_orders WHERE item_id = #{itemId}")
    int deleteByItemId(@Param("itemId") Long itemId);
```

### 两个 GatewayImpl 追加实现

```java
    // FlashItemGatewayImpl
    @Override
    public void resetStock(Long itemId) {
        flashItemMapper.resetStock(itemId);
    }

    // FlashOrderGatewayImpl
    @Override
    public void deleteByItemId(Long itemId) {
        flashOrderMapper.deleteByItemId(itemId);
    }
```

### FlashSaleGuard 追加

```java
    /**
     * 清除售罄标记。正常交易流「只进不出」；仅演示重置等库存回补类运维动作调用，
     * 否则重置后所有请求会被第一道防线误杀（一律 6103）。
     */
    public void clearSoldOut(Long itemId) {
        soldOutItems.computeIfAbsent(itemId, k -> new AtomicBoolean()).set(false);
    }
```

### FlashSaleService（client）追加

```java
    /**
     * 演示专用：重置商品库存回满、清空其订单并清除售罄标记，
     * 使前端并发演示可反复运行。仅进行中的活动可重置（否则 417）；
     * 生产环境此类运维动作必须加管理权限并审计。
     */
    void resetDemoItem(Long itemId);
```

### FlashSaleServiceImpl 追加（myOrders 之后）

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetDemoItem(Long itemId) {
        FlashItem item = flashItemGateway.findById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "秒杀商品不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime()) || now.isAfter(item.getEndTime())) {
            throw new BusinessException(ResultCodeEnum.VALIDATE_FAILED, "仅进行中的活动可重置演示数据");
        }
        flashItemGateway.resetStock(itemId);
        // 物理删除释放 uk(item_id, user_id)，否则重置后用户全部 6104
        flashOrderGateway.deleteByItemId(itemId);
        // 售罄标记只进不出是针对正常交易流；重置属于库存回补类运维动作，必须同步清除
        flashSaleGuard.clearSoldOut(itemId);
        log.info("演示数据已重置：itemId={}", itemId);
    }
```

### FlashSaleController 追加

```java
    /**
     * 演示专用：重置商品库存与订单，使前端并发演示可反复运行。
     * 生产环境此类运维动作必须加管理权限并审计，绝不对普通用户开放。
     */
    @PostMapping("/demo/reset/{itemId}")
    public AjaxResult<Void> resetDemo(@PathVariable Long itemId) {
        flashSaleService.resetDemoItem(itemId);
        return AjaxResult.success();
    }
```

### FlashSaleIntegrationTest 追加 Order(10)/Order(11)

```java
    @Test
    @Order(10)
    void demo_reset_restores_stock_and_frees_limit() {
        // Order(8) 已把商品 2 抢空并打上售罄标记——重置应全部复原
        String token = tokenForNewUser("rst");
        assertThat(post("/flash/items/2/seckill", Map.of(), token).get("code")).isEqualTo(6103);

        Map<String, Object> reset = post("/flash/demo/reset/2", Map.of(), token);
        assertThat(reset.get("code")).isEqualTo(200);

        // 同一用户首抢成功（库存回满 + 唯一索引已物理释放 + 售罄标记已清除）、二抢 6104
        assertThat(post("/flash/items/2/seckill", Map.of(), token).get("code")).isEqualTo(200);
        assertThat(post("/flash/items/2/seckill", Map.of(), token).get("code")).isEqualTo(6104);

        List<Map<String, Object>> items = dataList(get("/flash/items", loginDemo()));
        Map<String, Object> item2 = items.stream().filter(i -> i.get("id").equals(2)).findFirst().orElseThrow();
        assertThat(item2.get("stock")).isEqualTo(4);
    }

    @Test
    @Order(11)
    void demo_reset_ended_item_rejected() {
        Map<String, Object> body = post("/flash/demo/reset/4", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(417);
    }
```

### verify.sh：头部注意事项更新 + 追加第 8/9 步

```bash
#   2. 第 8 步会重置耳机（id=2）的库存与订单，脚本可重复运行（无需重启后端）

step "8. 重置演示库存（耳机回到 5 件，脚本可重复运行）"
CODE=$(curl -s -X POST "$BASE/flash/demo/reset/2" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE" != "200" ]; then fail "期望重置成功 200，实际 $CODE"; fi
STOCK2=$(curl -sf "$BASE/flash/items" -H "Authorization: Bearer $ACCESS" \
  | python -c "import sys,json; d=json.load(sys.stdin); print([i['stock'] for i in d['data'] if i['id']==2][0])")
if [ "$STOCK2" != "5" ]; then fail "重置后耳机库存应为 5，实际 $STOCK2"; fi
echo "重置成功，耳机库存回满 $STOCK2 ✓"

step "9. 已结束活动不可重置（期望 code=417）"
CODE=$(curl -s -X POST "$BASE/flash/demo/reset/4" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE" != "417" ]; then fail "期望 417，实际 $CODE"; fi
echo "状态守卫生效（417）✓"
```

design.md 同步：接口契约表增重置行与演示控制台说明、安全边界增演示入口警示、验证策略增第 7 条。

## 验证

| 验证方式 | 命令或步骤 | 结果 |
|----------|------------|------|
| test | `cd backend && mvn test` | 22/22 通过（Order 10/11 新用例覆盖三件套复位与状态守卫） |
| smoke | `FLASH_BASE=…8081/api bash scenarios/03-flash-sale/verify.sh` 连跑两遍 | 9/9 × 2，同一后端无需重启 |

## 风险与阻塞

- 阻塞：无。
- 执行记录：2026-09-07 落盘并验证；重置三件套缺一不可的结论（逻辑删除不释放唯一索引、售罄标记不清则误杀）已写入 NOTES.md 实现要点。
