package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.flash.FlashItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀商品 Mapper
 */
public interface FlashItemMapper extends BaseMapperPlus<FlashItem> {

    /**
     * 防超卖核心：判断（stock > 0）与扣减（stock - 1）在同一条语句内原子完成。
     * 数据库对单行更新串行执行，不存在「先查到 1 再决定扣」的竞态窗口；
     * 应用层不依赖任何先前读到的库存值，只认影响行数。
     */
    @Update("UPDATE flash_items SET stock = stock - 1, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{itemId} AND stock > 0 AND deleted = 0")
    int deductStock(@Param("itemId") Long itemId);

    /**
     * 演示专用：库存回满到初始值（生产环境没有「把库存加回去」这种运维入口，
     * 对应的是补货/回滚等专门流程）。
     */
    @Update("UPDATE flash_items SET stock = total_stock, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{itemId} AND deleted = 0")
    int resetStock(@Param("itemId") Long itemId);
}
