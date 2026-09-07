package com.fullstack.demo.domain.flash.gateway;

import com.fullstack.demo.domain.flash.FlashItem;

import java.util.List;

/**
 * 秒杀商品领域网关（仓储接口）。
 * <p>
 * 接口位于 domain，实现位于 demo-infrastructure（FlashItemGatewayImpl）。
 */
public interface FlashItemGateway {

    /**
     * 全量商品（按 id 升序）。
     */
    List<FlashItem> findAll();

    /**
     * 按主键查询。
     */
    FlashItem findById(Long id);

    /**
     * 原子扣减一件库存（防超卖第二道防线）。
     * <p>
     * 单语句完成「判断 + 扣减」：UPDATE ... SET stock = stock - 1
     * WHERE id = ? AND stock > 0。返回影响行数：1=成功；0=此刻库存为 0（或商品不存在）。
     * 调用方只认影响行数，不认先前查到的库存值。
     */
    int deductStock(Long itemId);

    /**
     * 演示专用：库存回满到初始值 total_stock（配合订单清理与售罄标记清除使用）。
     */
    void resetStock(Long itemId);
}
