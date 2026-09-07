package com.fullstack.demo.domain.flash;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fullstack.common.mybatisplus.entity.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体（场景 03：秒杀抢购）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flash_items")
public class FlashItem extends BaseIdEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品名
     */
    private String name;

    /**
     * 秒杀价（单位：分）
     */
    private Long price;

    /**
     * 原价（单位：分）
     */
    private Long originalPrice;

    /**
     * 初始库存（对账基准：成功订单数 + stock 应恒等于 total_stock）
     */
    private Integer totalStock;

    /**
     * 剩余库存
     */
    private Integer stock;

    /**
     * 开抢时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
