package com.fullstack.demo.domain.flash.gateway;

import com.fullstack.demo.domain.flash.FlashOrder;

import java.util.List;

/**
 * 秒杀订单领域网关（仓储接口）。
 */
public interface FlashOrderGateway {

    /**
     * 新增订单（回填雪花 ID；撞 (item_id, user_id) 唯一索引抛 DuplicateKeyException）。
     */
    void insert(FlashOrder order);

    /**
     * 查询用户在某商品上的订单（限购查重用，仅优化非兜底）。
     */
    FlashOrder findByItemAndUser(Long itemId, Long userId);

    /**
     * 用户的全部订单（创建时间倒序）。
     */
    List<FlashOrder> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 演示专用：物理删除某商品的全部订单——逻辑删除不会释放
     * uk(item_id, user_id) 唯一索引，重置后用户将无法再次抢购。
     */
    void deleteByItemId(Long itemId);
}
