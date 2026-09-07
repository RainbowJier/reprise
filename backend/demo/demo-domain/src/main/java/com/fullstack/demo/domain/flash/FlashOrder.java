package com.fullstack.demo.domain.flash;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fullstack.common.mybatisplus.entity.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 秒杀订单实体（场景 03：秒杀抢购）。
 * <p>
 * (item_id, user_id) 唯一索引：每人限购一件的物理保证；
 * item_name / price 为下单时快照，商品后续改名改价不影响已下订单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flash_orders")
public class FlashOrder extends BaseIdEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 秒杀商品 ID
     */
    private Long itemId;

    /**
     * 下单用户 ID
     */
    private Long userId;

    /**
     * 商品名快照
     */
    private String itemName;

    /**
     * 成交价快照（单位：分）
     */
    private Long price;

    /**
     * 订单状态：0=已抢购（预留 1=已取消/回补库存）
     */
    private Integer status;
}
