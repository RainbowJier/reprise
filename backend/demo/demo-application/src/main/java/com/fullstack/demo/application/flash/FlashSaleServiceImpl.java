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
