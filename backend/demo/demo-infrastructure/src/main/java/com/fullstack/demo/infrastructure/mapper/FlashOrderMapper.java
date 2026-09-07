package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.flash.FlashOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀订单 Mapper
 */
public interface FlashOrderMapper extends BaseMapperPlus<FlashOrder> {

    /**
     * 演示专用：物理删除。必须用原生 DELETE 而不是 MP 的逻辑删除——
     * 逻辑删除只置 deleted=1，唯一索引仍占用，用户重置后将永远 6104。
     */
    @Delete("DELETE FROM flash_orders WHERE item_id = #{itemId}")
    int deleteByItemId(@Param("itemId") Long itemId);
}
