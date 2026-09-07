---
id: flash-sale_03_service_seckill_core
name: "秒杀服务实现（防超卖三层防线 + 同事务回滚）"
type: service
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_01_data_model_flash_tables, flash-sale_02_contract_flash_contract]
profiles: [backend]
files:
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/flash/FlashSaleGuard.java
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/flash/FlashSaleServiceImpl.java
---

# 秒杀服务实现（防超卖三层防线 + 同事务回滚）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `service` |
| 已启用 profile | backend |
| 架构边界 | demo-application（用例编排，只依赖 domain Gateway 端口与 client 契约，不触碰 infrastructure） |
| 结构分析 | 沿用场景 01 模式：UserContextHolder（demo-shared）取登录态；BusinessException + ResultCodeEnum/61xx 抛错 |
| 完成条件 | `mvn test` 通过 |

## 需求与验收

- 用户目标：实现任务 02 的三个接口；seckill 的正确性核心——扣库存与插订单同事务、原子条件更新、唯一索引兜底、内存售罄标记拦洪峰。
- 包含：FlashSaleGuard（内存售罄标记）、FlashSaleServiceImpl。
- 不包含：Controller 与过滤器扩展（任务 04）。
- 验收：`mvn test` 通过；扣减失败路径（售罄）与插入冲突路径（重复抢购）语义正确。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 事务边界 | `@Transactional(rollbackFor = Exception.class)` 只加在 seckill：扣库存与插订单必须同生共死；列表/查询不加事务 |
| 三层防线 | ① FlashSaleGuard 内存标记（性能闸门，不承担正确性）→ ② deductStock 原子条件更新（只认影响行数）→ ③ uk(item_id,user_id) 唯一索引（DuplicateKeyException → 6104，事务回滚连带恢复库存） |
| 查重顺序 | findByItemAndUser 前置查重只是优化（减少无效扣减）；并发下仍可能同时通过查重，唯一索引是物理兜底 |
| 已知局限 | 内存标记单机有效、只进不出（无回补场景）；写入设计文档第七节 |
| 依赖 | 01（Gateway）、02（契约）；被 04（Controller）、05（测试）依赖 |

## 实现步骤

1. application 新建 flash 包：FlashSaleGuard；
2. 新建 FlashSaleServiceImpl（实现 client 的 FlashSaleService）；
3. `mvn test` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-application/.../application/flash/FlashSaleGuard.java` | application | 新增 | 内存售罄标记 |
| `backend/demo/demo-application/.../application/flash/FlashSaleServiceImpl.java` | application | 新增 | 秒杀服务实现 |

## 完整代码（供手动敲写）

### backend/demo/demo-application/src/main/java/com/fullstack/demo/application/flash/FlashSaleGuard.java（新增）

```java
package com.fullstack.demo.application.flash;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存售罄标记（防超卖第一道防线）：每商品一个 AtomicBoolean，
 * 已确认售罄的商品 O(1) 直接拒绝，请求不再触达数据库。
 * <p>
 * 只做性能闸门，不承担正确性：标记丢失（多实例间不共享）只会多几次
 * 数据库原子更新（由 WHERE stock > 0 兜底），绝不会超卖。
 * 标记只进不出：本场景无库存回补；接入回补时须在回补处同步清除标记。
 */
@Component
public class FlashSaleGuard {

    private final ConcurrentHashMap<Long, AtomicBoolean> soldOutItems = new ConcurrentHashMap<>();

    /**
     * 商品是否已标记售罄。
     */
    public boolean isSoldOut(Long itemId) {
        AtomicBoolean flag = soldOutItems.get(itemId);
        return flag != null && flag.get();
    }

    /**
     * 标记售罄（库存原子扣减影响行数为 0 时调用）。
     */
    public void markSoldOut(Long itemId) {
        soldOutItems.computeIfAbsent(itemId, k -> new AtomicBoolean()).set(true);
    }
}
```

### backend/demo/demo-application/src/main/java/com/fullstack/demo/application/flash/FlashSaleServiceImpl.java（新增）

```java
package com.fullstack.demo.application.flash;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.flash.FlashErrorCodes;
import com.fullstack.demo.client.dto.flash.FlashItemResp;
import com.fullstack.demo.client.dto.flash.FlashOrderResp;
import com.fullstack.demo.client.service.FlashSaleService;
import com.fullstack.demo.domain.flash.FlashItem;
import com.fullstack.demo.domain.flash.FlashOrder;
import com.fullstack.demo.domain.flash.gateway.FlashItemGateway;
import com.fullstack.demo.domain.flash.gateway.FlashOrderGateway;
import com.fullstack.demo.shared.auth.LoginUser;
import com.fullstack.demo.shared.auth.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 秒杀服务实现。防超卖三层防线：
 * <ol>
 *   <li>内存售罄标记——拦截洪峰中注定失败的请求（性能层）；</li>
 *   <li>数据库原子条件更新——判断与扣减单语句完成，只认影响行数（正确性基石）；</li>
 *   <li>唯一索引 (item_id, user_id)——限购幂等的物理兜底；插入冲突时整个事务回滚，
 *       扣掉的库存一并恢复（扣库存与建订单同生共死）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements FlashSaleService {

    private final FlashItemGateway flashItemGateway;
    private final FlashOrderGateway flashOrderGateway;
    private final FlashSaleGuard flashSaleGuard;

    @Override
    public List<FlashItemResp> listItems() {
        Long userId = currentUserId();
        // 一次性取当前用户全部订单构建已抢集合，避免逐商品查询
        Set<Long> mineItemIds = flashOrderGateway.findByUserIdOrderByCreateTimeDesc(userId)
                .stream().map(FlashOrder::getItemId).collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        return flashItemGateway.findAll().stream()
                .map(item -> toResp(item, statusOf(item, now), mineItemIds.contains(item.getId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlashOrderResp seckill(Long itemId) {
        Long userId = currentUserId();

        FlashItem item = flashItemGateway.findById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "秒杀商品不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            throw new BusinessException(FlashErrorCodes.NOT_STARTED, "活动未开始");
        }
        if (now.isAfter(item.getEndTime())) {
            throw new BusinessException(FlashErrorCodes.ENDED, "活动已结束");
        }

        // 防线一：内存售罄标记，已售罄 O(1) 拒绝，不再触库
        if (flashSaleGuard.isSoldOut(itemId)) {
            throw new BusinessException(FlashErrorCodes.SOLD_OUT, "已售罄，手慢了");
        }

        // 查重只是优化（减少无效扣减与行锁竞争），限购的物理保证是唯一索引
        if (flashOrderGateway.findByItemAndUser(itemId, userId) != null) {
            throw new BusinessException(FlashErrorCodes.DUPLICATE, "每人限购一件，请勿重复抢购");
        }

        // 防线二：原子条件更新——判断与扣减在一条语句内完成，只认影响行数
        if (flashItemGateway.deductStock(itemId) == 0) {
            flashSaleGuard.markSoldOut(itemId);
            throw new BusinessException(FlashErrorCodes.SOLD_OUT, "已售罄，手慢了");
        }

        // 防线三：唯一索引兜底并发重复下单；冲突时本事务回滚，已扣库存一并恢复
        FlashOrder order = new FlashOrder();
        order.setItemId(itemId);
        order.setUserId(userId);
        order.setItemName(item.getName());
        order.setPrice(item.getPrice());
        order.setStatus(0);
        try {
            flashOrderGateway.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(FlashErrorCodes.DUPLICATE, "每人限购一件，请勿重复抢购");
        }
        log.info("秒杀成功：itemId={}, userId={}, orderId={}", itemId, userId, order.getId());
        return toResp(order);
    }

    @Override
    public List<FlashOrderResp> myOrders() {
        return flashOrderGateway.findByUserIdOrderByCreateTimeDesc(currentUserId())
                .stream().map(this::toResp).toList();
    }

    private Long currentUserId() {
        LoginUser login = UserContextHolder.get();
        if (login == null) {
            // 过滤器保证上下文存在，此处兜底防御
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        return login.getUserId();
    }

    private String statusOf(FlashItem item, LocalDateTime now) {
        if (now.isBefore(item.getStartTime())) {
            return "NOT_STARTED";
        }
        if (now.isAfter(item.getEndTime())) {
            return "ENDED";
        }
        return "IN_PROGRESS";
    }

    private FlashItemResp toResp(FlashItem item, String status, boolean mine) {
        return new FlashItemResp(item.getId(), item.getName(), item.getPrice(), item.getOriginalPrice(),
                item.getTotalStock(), item.getStock(), item.getStartTime(), item.getEndTime(), status, mine);
    }

    private FlashOrderResp toResp(FlashOrder order) {
        return new FlashOrderResp(order.getId(), order.getItemId(), order.getItemName(),
                order.getPrice(), order.getStatus(), order.getCreateTime());
    }
}
```

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全部通过（Bean 装配 + 既有用例回归） |

## 风险与阻塞

- 风险：DuplicateKeyException 依赖 Spring SQL 错误码翻译（H2 23505 → DuplicateKey，场景 01 注册重名已验证同机制）；并发正确性由任务 05 的并发集成测试覆盖。
- 阻塞：无。
- 执行记录：2026-09-07 全部落盘，与任务文件代码一致；`mvn test` 通过（并发正确性由任务 05 的 Order(8)/Order(9) 用例覆盖：200 用户抢 5 库存恰 5 成功、单用户 20 线程并发恰好 1 单）。
