package com.fullstack.demo.client.dto.flash;

import java.time.LocalDateTime;

/**
 * 秒杀订单。
 *
 * @param orderId    订单 ID（雪花）
 * @param itemId     商品 ID
 * @param itemName   商品名快照
 * @param price      成交价快照（单位：分）
 * @param status     订单状态：0=已抢购
 * @param createTime 下单时间
 */
public record FlashOrderResp(
        Long orderId,
        Long itemId,
        String itemName,
        Long price,
        Integer status,
        LocalDateTime createTime
) {
}
