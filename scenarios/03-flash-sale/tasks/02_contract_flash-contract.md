---
id: flash-sale_02_contract_flash_contract
name: "秒杀接口契约（DTO + FlashSaleService 端口 + 61xx 错误码）"
type: contract
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_01_data_model_flash_tables]
profiles: [backend, api]
files:
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/flash/FlashErrorCodes.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/flash/FlashItemResp.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/flash/FlashOrderResp.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/FlashSaleService.java
---

# 秒杀接口契约（DTO + FlashSaleService 端口 + 61xx 错误码）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `contract` |
| 已启用 profile | backend / api |
| 架构边界 | demo-client（服务接口 `client/service/` + DTO `client/dto/flash/`），实现后续落在 demo-application（任务 03） |
| 结构分析 | 沿用场景 01 契约模式：接口与 DTO 在 client 层冻结，前端任务 06 依据同一契约并行 |
| 完成条件 | `mvn test` 编译通过（接口尚无实现，Controller 未建） |

## 需求与验收

- 用户目标：冻结三个接口的契约——列表 / 抢购 / 我的订单；秒杀域错误码 61xx 分段；金额单位分。
- 包含：FlashItemResp / FlashOrderResp record、FlashErrorCodes 常量、FlashSaleService 接口。
- 不包含：实现（任务 03）与 Controller（任务 04）。
- 验收：`mvn test` 通过。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 错误码 | 6101 未开始 / 6102 已结束 / 6103 已售罄 / 6104 重复抢购；`BusinessException(String code, msg)` + GlobalExceptionHandler 的 `buildCodeAndMessage` 原生支持任意数字码，前端 http.js 按 `code !== 200` 统一 reject 展示 msg |
| DTO 风格 | record（不可变、Jackson 原生序列化；同场景 01 UserInfoResp） |
| 状态语义 | `NOT_STARTED` / `IN_PROGRESS` / `ENDED` 由服务端按 now 与 start/end 推导，前端不做时间判断（服务端才是真相） |
| 依赖 | 01（实体语义）；被 03（实现）、04（Controller）、06（前端 api 模块）依赖 |

## 实现步骤

1. client 新建 `dto/flash` 包：FlashErrorCodes、FlashItemResp、FlashOrderResp；
2. client 新建 `service/FlashSaleService.java`；
3. `mvn test` 验证编译。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-client/.../client/dto/flash/FlashErrorCodes.java` | client | 新增 | 61xx 错误码常量 |
| `backend/demo/demo-client/.../client/dto/flash/FlashItemResp.java` | client | 新增 | 商品列表项 DTO |
| `backend/demo/demo-client/.../client/dto/flash/FlashOrderResp.java` | client | 新增 | 订单 DTO |
| `backend/demo/demo-client/.../client/service/FlashSaleService.java` | client | 新增 | 服务端口 |

## 完整代码（供手动敲写）

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/flash/FlashErrorCodes.java（新增）

```java
package com.fullstack.demo.client.dto.flash;

/**
 * 秒杀域业务错误码（61xx 分段）。
 * <p>
 * 与通用 HTTP 语义码（401/404/409…）区分开：秒杀的「失败」大多是正常业务结果
 * （手慢了、重复抢），不是客户端错误。前端按 code !== 200 统一拒绝并直接展示 msg。
 */
public final class FlashErrorCodes {

    /** 活动未开始 */
    public static final String NOT_STARTED = "6101";

    /** 活动已结束 */
    public static final String ENDED = "6102";

    /** 已售罄 */
    public static final String SOLD_OUT = "6103";

    /** 重复抢购（每人限购一件） */
    public static final String DUPLICATE = "6104";

    private FlashErrorCodes() {
    }
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/flash/FlashItemResp.java（新增）

```java
package com.fullstack.demo.client.dto.flash;

import java.time.LocalDateTime;

/**
 * 秒杀商品列表项。
 *
 * @param id            商品 ID
 * @param name          商品名
 * @param price         秒杀价（单位：分）
 * @param originalPrice 原价（单位：分）
 * @param totalStock    初始库存
 * @param stock         剩余库存
 * @param startTime     开抢时间
 * @param endTime       结束时间
 * @param status        活动状态：NOT_STARTED / IN_PROGRESS / ENDED（服务端推导）
 * @param mine          当前用户是否已抢购（限购一件，已抢则前端按钮置灰）
 */
public record FlashItemResp(
        Long id,
        String name,
        Long price,
        Long originalPrice,
        Integer totalStock,
        Integer stock,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        boolean mine
) {
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/flash/FlashOrderResp.java（新增）

```java
package com.fullstack.demo.client.dto.flash;

import java.time.LocalDateTime;

/**
 * 秒杀订单。
 *
 * @param orderId     订单 ID（雪花）
 * @param itemId      商品 ID
 * @param itemName    商品名快照
 * @param price       成交价快照（单位：分）
 * @param status      订单状态：0=已抢购
 * @param createTime  下单时间
 */
public record FlashOrderResp(
        Long orderId,
        Long itemId,
        String itemName,
        Long price,
        Integer status,
        LocalDateTime createTime
) {
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/FlashSaleService.java（新增）

```java
package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.flash.FlashItemResp;
import com.fullstack.demo.client.dto.flash.FlashOrderResp;

import java.util.List;

/**
 * 秒杀服务端口（场景 03：秒杀抢购）。
 * <p>
 * 当前用户取自 UserContextHolder（/flash/* 由 JwtAuthFilter 注入上下文），
 * 实现位于 demo-application（任务 03）。
 */
public interface FlashSaleService {

    /**
     * 秒杀商品列表：实时库存、服务端推导的活动状态、当前用户的已抢标记。
     */
    List<FlashItemResp> listItems();

    /**
     * 抢购下单。失败抛 BusinessException：
     * 404 商品不存在 / 6101 未开始 / 6102 已结束 / 6103 已售罄 / 6104 重复抢购。
     */
    FlashOrderResp seckill(Long itemId);

    /**
     * 当前用户的抢购订单（时间倒序）。
     */
    List<FlashOrderResp> myOrders();
}
```

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 编译通过（接口无实现不触发装配错误） |

## 风险与阻塞

- 风险：无。
- 阻塞：无。
- 执行记录：2026-09-07 全部落盘，与任务文件代码一致；`mvn test` 通过。
