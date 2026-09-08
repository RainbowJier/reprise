package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.flash.FlashItem;

/**
 * 秒杀商品 Mapper
 * <p>
 * SQL 位于同包名 XML：resources/mapper/FlashItemMapper.xml。
 */
public interface FlashItemMapper extends BaseMapperPlus<FlashItem> {

    /**
     * 防超卖核心：判断（stock > 0）与扣减（stock - 1）在 XML SQL 中同一条语句内原子完成。
     */
    int deductStock(Long itemId);

    /**
     * 演示专用：库存回满到初始值。
     */
    int resetStock(Long itemId);
}
