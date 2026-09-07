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
